# Camel-Openstack component
This is standard camel-openstack component enlarged about integration test suite.

You can find all the integration tests in `test` folder in package `org.apache.camel.component.openstack.it`.

## Confuguration
Integration tests are skipped by default because it needs running OpenStack instance and several configuration parameters.

Please configure following parameters in **OpenstackTestProperties.properties** file:

Logging parameters:
- **OPENSTACK_URI** - URL of openstack instance (example: `http://127.0.0.1:5000/v3`)
- **OPENSTACK_USERNAME** - OpenStack username (will be used by IT tests)
- **OPENSTACK_PASSWORD** - password
- **PROJECT_ID** - test project ID

Following parameters are used in ServerITTest:
- **IMAGE_ID** - image ID
- **FLAVOR_ID** - flavor ID
- **NETWORK_ID** network ID

## Run Integration Tests
Build project with `it-test` profile

```
mvn clean install -Pit-test
```
## Troubleshooting
Integration tests strongly relies on OpenStack configuration - it is possible that some test fails. In that case revise your OpenStack instance configuration. Especially available resources etc.
