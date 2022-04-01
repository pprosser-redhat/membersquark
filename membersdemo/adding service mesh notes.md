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

## added initial virtual service
````
kind: VirtualService
apiVersion: networking.istio.io/v1alpha3
metadata:
  name: members
  namespace: membersapp
spec:
  hosts:
    - members.apps.coffee.demolab.local
  gateways:
    - members
  http:
    - match:
        - uri:
            prefix: /membersweb/rest/members
      route:
        - destination:
            host: membersdemov1
            port:
              number: 8080

````
## Gateway

````
kind: Gateway
apiVersion: networking.istio.io/v1alpha3
metadata:
  name: members
  namespace: membersapp
spec:
  servers:
    - hosts:
        - members.apps.coffee.demolab.local
      port:
        name: http
        number: 80
        protocol: HTTP
  selector:
    istio: ingressgateway

````
## RHSSO
````
kind: RequestAuthentication
apiVersion: security.istio.io/v1beta1
metadata:
  name: rhsso
  namespace: membersapp
spec:
  selector:
    matchLabels:
      app: members
  jwtRules:
    - issuer: 'http://sso-sso-clear.apps.coffee.demolab.local/auth/realms/3scale'
      jwksUri: >-
        http://sso-sso-clear.apps.coffee.demolab.local/auth/realms/3scale/protocol/openid-connect/certs

````
## Config Map
````
kind: ConfigMap
apiVersion: v1
metadata:
  name: member-config
  namespace: membersapp
data:
  quarkus.datasource.jdbc.url: 'jdbc:postgresql://postgresql:5432/sampledb'
  quarkus.datasource.password: phil
  quarkus.datasource.username: phil
  quarkus.hibernate-orm.database.generation: drop-and-create

````
## deployment type

quarkus.openshift.deployment-kind=Deployment

## image group (defaults to user name, need it to be my namespace)

quarkus.container-image.group=membersapp
