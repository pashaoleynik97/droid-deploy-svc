# Dev Notes

## Docker/deployment implications (just to remember)

```yaml
services:
  droiddeploy:
    image: your/droiddeploy:latest
    volumes:
      - /srv/droiddeploy/apks:/var/lib/droiddeploy/apks
    environment:
      - DROIDDEPLOY_STORAGE_ROOT=/var/lib/droiddeploy/apks # or from YAML
```

- /srv/droiddeploy/apks can be on any storage

## Tagging

```text
git tag -a v0.1.0 -m "Release version 0.1.0"              
git push origin v0.1.0 
```