#!/usr/bin/dumb-init /bin/sh
rm -f /opt/healthcheck

vault server \
        -dev-root-token-id="${VAULT_DEV_ROOT_TOKEN_ID:-root}" \
        -dev-listen-address="${VAULT_DEV_LISTEN_ADDRESS:-"0.0.0.0:8200"}" \
        -dev "$@" &

sleep 1 # wait for Vault to come up
export VAULT_ADDR='http://127.0.0.1:8200'
# add  shakti-registration service keys

echo "writing value to secret/shakti-registration/dev/"
vault kv put secret/shakti-registration/dev/  spring.couchbase.username=someuser
vault kv patch secret/shakti-registration/dev/  spring.couchbase.password=password123
vault kv patch secret/shakti-registration/dev/  gluu.uri=https://iam-dev2.shakticoin.com
vault kv patch secret/shakti-registration/dev/  gluu.client.id=6d6e7ace-3b3a-48ad-be22-d2146f314927
vault kv patch secret/shakti-registration/dev/  gluu.client.secret=vTIsFJr3ccVhvXjEHp0P8K8WuMPDQOyYAC4Iein1
echo "finished writing value to secret/shakti-registration/dev/"

# block forever
tail -f /dev/null