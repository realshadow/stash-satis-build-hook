# Satis Build Hook

Satis Build Hook is a post receive hook for Atlassian Stash that works together with [Satis Control Panel (SCP)](https://github.com/realshadow/satis-control-panel).

## How it works

When run, hook will first register the repository in SCP and then trigger partial update of Satis repository, thus 
rebuilding only the repository that triggered this hook. 

Partial update can be disabled in hook settings and is disabled by default, because it considers a CI server in your setup.
If you wish to rebuild your repository after push you can easily enable it in hook settings. E.g. if you have a separate
git mirror of a public package and you do not want you to pass it through your CI server.

## Global configuration

SCP API URL address can be configured in global settings and it should point to API 
endpoint, e.g. *http://example.com/control-panel/api/repository*
