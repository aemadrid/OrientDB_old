require 'wrest'
require 'nokogiri'
require 'fileutils'

class Page
  attr_reader :original_url, :url
  attr_reader :title, :name
  attr_writer :original_html, :html
  attr_writer :original_doc, :doc, :links

  SITE = 'http://code.google.com' unless const_defined?(:SITE)
  MARK = '/p/orient/wiki/' unless const_defined?(:MARK)
  ROOT_URL = "#{SITE}#{MARK}Main?tm=6" unless const_defined?(:ROOT_URL)
  CONTENT_ID = 'wikimaincol' unless const_defined?(:CONTENT_ID)

  def initialize(name, original_url, title = nil)
    @name = name
    @original_url = original_url
    @title = title if title
    @html = nil
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
    @links ||= {}
  end

  def processed?
    @processed
  end

  def ready?
    !processed?
  end

  def process
    log "[#{name} : process] running [#{name}] ..."
    clean_html
    find_links
    log "[#{name} : process] processed [#{name}] ..."
    @processed = true
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
          links[href] = title
          new_url, section = self.class.split_url href
          link["href"] = "#{new_url}.html#{section ? "##{section}" : ""}"
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
    log "[#{name} : clean_html] Replacing hs ..."
    doc.css('h5').each { |tag| tag.name 'h6' rescue nil }
    doc.css('h4').each { |tag| tag.name 'h5' rescue nil }
    doc.css('h3').each { |tag| tag.name 'h4' rescue nil }
    doc.css('h2').each { |tag| tag.name 'h3' rescue nil }
    doc.css('h1').each { |tag| tag.name 'h2' rescue nil }
  end

  def save_original
    File.open("_original/#{name}.html", "w") do |f|
      f.puts original_html
    end
  end

  def save_generated
    File.open("_generated/#{name}.html", "w") do |f|
      f.puts "---\ntitle: #{title}\nlayout: default\n---"
      f.puts doc.to_html
    end
  end

  def log(*args)
    self.class.log(*args)
  end

  class << self
    def migrate(do_all = true)
      log "*" * 120
      log " [ MIGRATING ] ".center(120, "*")
      log "*" * 120
      all.clear
      cmd = "rm -rf _generated/*.html"
      puts "[CMD] #{cmd}\n#{`#{cmd}`}"
      add ROOT_URL, "Main"
      last = true
      while do_all && last
        last = process_next
      end
      if all
        cmd = "mv _generated/*.html ."
        puts "[CMD] #{cmd}\n#{`#{cmd}`}"
        cmd = "jekyll"
        puts "[CMD] #{cmd}\n#{`#{cmd}`}"
      end
    end

    def stats
      h = {:processed => 0, :ready => 0, :total => 0}
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
      found.first.process
      true
    end

    def all
      @all ||= {}
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
      name = name.underscore.downcase
      log "[split_url] [#{url}] name [#{name}] section [#{section}] ..."
      [name, section]
    end

    def log(msg)
      puts "#{msg}"
    end
  end
end

desc "Migrate all files from the original wiki into github pages"
task :migrate do
  Page.migrate
end