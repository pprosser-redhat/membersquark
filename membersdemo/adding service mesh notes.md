## PostgreSQL

added

      annotations:
        sidecar.istio.io/inject: 'true'

to template.metadata

## Property added to application.properties 

quarkus.openshift.annotations."sidecar.istio.io/inject"=true

## added app and version labels  to application.properties

quarkus.openshift.labels.app=members
quarkus.openshift.labels.version=v1