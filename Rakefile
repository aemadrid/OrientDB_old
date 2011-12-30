Object.const_set(:ROOT_PATH, File.dirname(__FILE__)) unless Object.const_defined?(:ROOT_PATH)
require File.join ROOT_PATH, '_lib', 'page'

desc "Migrate all files from the original wiki into github pages"
task :migrate do
  Page.migrate
end

desc "Generate the topbar menu"
task :menu do
  Menu.generate
end