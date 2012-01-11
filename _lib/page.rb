require 'wrest'
require 'nokogiri'
require 'tidy_ffi'
require 'fileutils'

Object.const_set(:ROOT_PATH, File.expand_path('..', File.dirname(__FILE__))) unless Object.const_defined?(:ROOT_PATH)
require File.join ROOT_PATH, '_lib', 'menu'

class Page

  attr_reader :original_url, :url
  attr_reader :title, :name
  attr_writer :original_html, :html
  attr_writer :original_doc, :doc, :links

  SITE = 'http://code.google.com' unless const_defined?(:SITE)
  MARK = '/p/orient/wiki/' unless const_defined?(:MARK)
  ROOT_URL = "#{SITE}#{MARK}Main?tm=6" unless const_defined?(:ROOT_URL)
  CONTENT_ID = 'wikimaincol' unless const_defined?(:CONTENT_ID)
  TIDY_OPTIONS = {
    :numeric_entities   => 1,
    :output_html        => 1,
    :merge_divs         => 0,
    #:merge_spans        => 0,
    :join_styles        => 0,
    :clean              => 1,
    :indent             => 1,
    :wrap               => 0,
    :drop_empty_paras   => 0,
    :literal_attributes => 1,
    :show_body_only     => 1
  } unless const_defined?(:TIDY_OPTIONS)

  def initialize(name, original_url, title = nil)
    @name         = name
    @original_url = original_url
    @title = title if title
    @html      = nil
    @processed = false
  end

  def full_url
    log "full_url : original_url : #{original_url}"
    return original_url if original_url.starts_with? SITE
    res = SITE + original_url
    log "full_url : res : #{res}"
    res
  end

  def original_html
    @original_html ||= full_url.to_uri.get.body
  end

  def original_doc
    @original_doc ||= Nokogiri::HTML original_html
  end

  def html
    unless @html.present?
      if found = original_doc.at_css("##{CONTENT_ID}")
        @html = found.to_html.gsub(%{<div class="vt" id="#{CONTENT_ID}">}, '')[0..-7]
      else
        @html = @original_html.gsub(%{<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN" "http://www.w3.org/TR/REC-html40/loose.dtd">\n}, '')
        @html.gsub! %{<html><body>}, ''
        @html.gsub! %{</body></html>}, ''
      end
    end
    @html
  end

  def doc
    @doc ||= Nokogiri::HTML::DocumentFragment.parse html
  end

  def links
    @links ||= { }
  end

  def processed?
    @processed
  end

  def ready?
    !processed?
  end

  def process(do_save = false)
    log "[#{name} : process] running [#{name}] ..."
    clean_html
    find_links
    log "[#{name} : process] processed [#{name}] ..."
    @processed = true
    save if do_save
  end

  def save
    save_original
    save_generated
  end

  def find_links
    found = doc.css 'a'
    log "[#{name} : find_links] Found #{found.size} links ..."
    found.each_with_index do |link, idx|
      href, title = link['href'], link.text
      log " [#{name} : find_links : #{idx + 1}/#{found.size} : #{href} ] ".center(120, '-')
      if href
        if href.starts_with? MARK
          log "[#{name} : find_links : link [#{link['href']}] (1) ..."
          log "[#{name} : find_links : #{idx + 1}/#{found.size}] Adding [#{href}] ..."
          links[href]      = title
          new_url, section = self.class.split_url href
          link["href"]     = "#{new_url}.html#{section ? "##{section}" : ""}"
          log "[#{name} : find_links : link [#{link['href']}] (2) ..."
          added = self.class.add href, title
        else
          log "[#{name} : find_links : #{idx + 1}/#{found.size}] Skipping [#{href}] ..."
        end
      else
        log "[#{name} : find_links : #{idx + 1}/#{found.size}] Skipping [#{link.to_html}] ..."
      end
    end
  end

  def clean_html
    # first empty/ul tags
    do_end = false
    cnt = 0
    until do_end
      cnt += 1
      tag = doc.children[0]
      case tag.name
        when "text", "p"
          if tag.text.strip.empty?
            tag.remove
            log "[#{name} : clean_html] (#{cnt}) #{tag.name.upcase} : removed : #{tag.to_html}"
          else
            if tag.children.size == 1 && tag.children.first.name == "img"
              log "[#{name} : clean_html] (#{cnt}) #{tag.name.upcase} : not empty but logo : #{tag.to_html}"
            else
              log "[#{name} : clean_html] (#{cnt}) #{tag.name.upcase} : not empty : #{tag.to_html}"
              do_end = true
            end
          end
        when "ul"
          tag.remove
          log "[#{name} : clean_html] (#{cnt}) #{tag.name.upcase} : removed : #{tag.to_html}"
          do_end = true
        else
          log "[#{name} : clean_html] (#{cnt}) #{tag.name.upcase} : unrecognized, finishing ..."
          do_end = true
      end
    end
    # h5, h4, h3, h2, h1
    [5, 4, 3, 2, 1].each do |nr|
      nodes = doc.css "h#{nr}"
      if nodes.size > 0
        nodes.each_with_index do |tag, idx|
          log "[#{name} : clean_html] (#{idx + 1}) Replacing h#{nr} with h#{nr + 1} ..."
          tag.name = "h#{nr + 1}"
        end
      else
        log "[#{name} : clean_html] No h#{nr} nodes found ..."
      end
    end
    # tables
    nodes = doc.css 'table.wikitable'
    if nodes.size > 0
      nodes.each_with_index do |tag, idx|
        log "[#{name} : clean_html] (#{idx + 1}) Replacing table styling  ..."
        tag['class'] = 'zebra-striped bordered-table'
      end
    else
      log "[#{name} : clean_html] No table.wikitable nodes found ..."
    end
    # td styling
    nodes = doc.css 'td'
    if nodes.size > 0
      nodes.each_with_index do |tag, idx|
        mark = 'border: 1px solid #ccc; padding: 5px;'
        if tag['style'] && tag['style'].index(mark)
          log "[#{name} : clean_html] (#{idx + 1}) Removing extra td styling ..."
          tag['style'] = tag['style'].gsub mark, ''
          tag.remove_attribute('style') if tag['style'].to_s.strip.empty?
        else
          log "[#{name} : clean_html] (#{idx + 1}) No need to remove extra td styling ..."
        end
      end
    else
      log "[#{name} : clean_html] No table.wikitable nodes found ..."
    end
    # Remove logos
    nodes = doc.css 'p img'
    if nodes.size > 0
      nodes.each_with_index do |tag, idx|
        if tag['src'] && tag['src'] == 'http://www.orientechnologies.com/images/orient_db_small.png'
          log "[#{name} : clean_html] (#{idx + 1}) Removing logo ..."
          tag.parent.remove
        else
          log "[#{name} : clean_html] (#{idx + 1}) No need to remove logo here ..."
        end
      end
    else
      log "[#{name} : clean_html] No logo nodes found ..."
    end
  end

  def breadcrumbs
    unless instance_variable_defined? :@breadcrumbs
      @breadcrumbs = []
      key          = name
      until key.nil?
        key = Menu.parents[key]
        if key && (parent_page = self.class.all[key])
          @breadcrumbs << [parent_page.name, parent_page.title]
          log "[#{name} : breadcrumbs] (#{@breadcrumbs.size}) #{@breadcrumbs.last.inspect} ..."
          key = parent_page.name
        end
      end
    end
    log "[#{name} : breadcrumbs] (final:#{@breadcrumbs.size}) #{@breadcrumbs.empty? ? 'N/A' : @breadcrumbs.map { |k, _| k }.join(" / ")} ..."
    @breadcrumbs
  end

  def save_original
    path = File.join ::ROOT_PATH, "_original", "#{name}.html"
    FileUtils.mkdir_p File.dirname path
    log "[#{name} : save_original] Saving original : #{path} ..."
    File.open(path, "w") do |f|
      f.puts original_html
    end
  end

  def save_generated
    path = File.join ::ROOT_PATH, "_generated", "#{name}.html"
    FileUtils.mkdir_p File.dirname path
    log "[#{name} : save_generated] Saving original : #{path} ..."
    File.open(path, "w") do |f|
      f.puts "---"
      f.puts "title: #{title}"
      f.puts "layout: default"
      breadcrumbs.each_with_index do |(name, title), idx|
        f.puts "bc_#{idx + 1}_name: #{name}"
        f.puts "bc_#{idx + 1}_title: #{title}"
      end
      f.puts "---"
      code_html = doc.to_html.to_s
      log "[#{name} : save_generated] code_html : #{code_html.size} ..."
      cleaned_html = TidyFFI::Tidy.new(code_html, Page::TIDY_OPTIONS).clean.to_s
      log "[#{name} : save_generated] cleaned_html : #{cleaned_html.size} ..."
      final_html = cleaned_html.size > 0 ? cleaned_html : code_html
      log "[#{name} : save_generated] final_html : #{final_html.size} ..."
      f.puts final_html
    end
  end

  def log(*args)
    self.class.log(*args)
  end

  class << self

    def cmd(str)
      puts "[CMD] #{str}\n#{`#{str}`}"
    end

    def clear
      all.clear
      cmd "rm -rf _generated/*.html"
    end

    def migrate(do_all = true)
      time = Time.now
      log "*" * 120
      log " [ MIGRATING ] ".center(120, "*")
      log "*" * 120
      clear
      add ROOT_URL, "Main"
      if do_all
        log " [ PROCESSING ALL ] ".center(120, "*")
        log "[migrate] Processing all ..."
        last = true
        while last
          log "[migrate] Processing next ..."
          last = process_next
        end
        log " [ SAVING ALL ] ".center(120, "*")
        all.each { |_, page| page.save }
        log " [ MOVING ALL ] ".center(120, "*")
        cmd "mv _generated/*.html ."
        log " [ JEKYLL ] ".center(120, "*")
        cmd "jekyll"
      end
      time = (Time.now - time).to_i
      log " [ THE END : #{'%i seconds' % time} ] ".center(120, "*")
      self[:main]
    end

    def stats
      h = { :processed => 0, :ready => 0, :total => 0 }
      all.each do |_, page|
        h[:processed] += 1 if page.processed?
        h[:ready] += 1 if page.ready?
        h[:total] += 1
      end
      h
    end

    def process_next
      found = ready
      log "[process_next : #{found.class} : #{found.size rescue 'ERR'}"
      return false if found.empty?
      log "[process_next : #{found.first.class} : #{found.first.name rescue 'ERR'} : #{found.size rescue 'ERR'}] processing ..."
      found.first.process false
      true
    end

    def all
      @all ||= { }
    end

    def [](name)
      all[name.to_s.underscore]
    end

    def processed
      all.map { |_, page| page.processed? ? page : nil }.compact
    end

    def ready
      all.map { |_, page| page.ready? ? page : nil }.compact
    end

    def add(url, title = nil)
      log "=" * 120
      log "[#{url}] Adding ... (#{title})"
      name, section = split_url url
      if all.key?(name)
        log "[#{name}] Already here ..."
      else
        log "[#{name}] Adding it ..."
        all[name] = new name, url, title
      end
      all[name]
    end

    def split_url(url)
      str = url.to_s.dup
      str.gsub! SITE, ''
      str.gsub! MARK, ''
      str.gsub! /\?(.*)$/, ''
      name, section = str.split "#"
      name          = name.underscore.downcase
      log "[split_url] [#{url}] name [#{name}] section [#{section}] ..."
      [name, section]
    end

    def log(msg)
      puts "#{msg}"
    end
  end
end
