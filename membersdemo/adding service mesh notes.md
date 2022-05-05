# Service Mesh on it's own
## PostgreSQL

added

      annotations:
        sidecar.istio.io/inject: 'true'

to template.metadata

## Property added to application.properties 

quarkus.openshift.annotations."sidecar.istio.io/inject"=true

## added app and version labels  to application.properties

quarkus.openshift.labels.app=members
quarkus.openshift.labels.version=v2
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

## Create a service that uses a selector to choose between both versions of the members Quarkus service

Doesn't seem to be a way in Quarkus properties to choose how to generate the K8s service. Will create a new service specific for my istio requirements

````
kind: Service
apiVersion: v1
metadata:
  name: members
  namespace: membersapp-mesh
  labels:
    discovery.3scale.net: 'true'
  annotations:
    discovery.3scale.net/description-path: /q/openapi?format=json
    discovery.3scale.net/port: '80'
    discovery.3scale.net/scheme: http
spec:
  ports:
    - name: http
      protocol: TCP
      port: 80
      targetPort: 8080
  selector:
    app: members

````

## Create destination route

````
kind: DestinationRule
apiVersion: networking.istio.io/v1alpha3
metadata:
  name: members
  namespace: membersapp
spec:
  host: members.membersapp.svc.cluster.local
  subsets:
    - labels:
        app: members
        version: v1
      name: v1
    - labels:
        app: members
        version: v2
      name: v2
````

## Gateway

````
kind: Gateway
apiVersion: networking.istio.io/v1alpha3
metadata:
  name: members-gateway
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
    - membersapp/members-gateway
  http:
    - match:
        - uri:
            exact: /v1/membersweb/rest/members
        - uri:
            prefix: /v1/membersweb/rest/members
      rewrite:
        uri: /membersweb/rest/members
      route:
        - destination:
            host: members.membersapp.svc.cluster.local
            subset: v1
          weight: 100
        - destination:
            host: members.membersapp.svc.cluster.local
            subset: v2
    - match:
        - uri:
            exact: /v2/membersweb/rest/members
        - uri:
            prefix: /v2/membersweb/rest/members
      rewrite:
        uri: /membersweb/rest/members
      route:
        - destination:
            host: members.membersapp.svc.cluster.local
            subset: v1
        - destination:
            host: members.membersapp.svc.cluster.local
            subset: v2
          weight: 100
    - match:
        - uri:
            exact: /q/metrics
      route:
        - destination:
            host: members.membersapp.svc.cluster.local
            subset: v1
          weight: 50
        - destination:
            host: members.membersapp.svc.cluster.local
            subset: v2
          weight: 50
````

## RHSSO - Request Authentication
````
kind: RequestAuthentication
apiVersion: security.istio.io/v1beta1
metadata:
  name: rhssocheck
  namespace: membersapp-mesh
spec:
  selector:
    matchLabels:
      app: members
  jwtRules:
    - issuer: 'http://sso-sso-clear.apps.coffee.demolab.local/auth/realms/3scale'
      jwksUri: >-
        http://sso-sso-clear.apps.coffee.demolab.local/auth/realms/3scale/protocol/openid-connect/certs


````
## Authorisation Policy
````
kind: AuthorizationPolicy
apiVersion: security.istio.io/v1beta1
metadata:
  name: membersauth
  namespace: membersapp-mesh
spec:
  selector:
    matchLabels:
      app: members
  rules:
    - to:
        - operation:
            methods:
              - GET
              - POST
              - DELETE
            paths:
              - /membersweb/*
      when:
        - key: 'request.auth.claims[iss]'
          values:
            - 'http://sso-sso-clear.apps.coffee.demolab.local/auth/realms/3scale'
        - key: 'request.auth.claims[azp]'
          values:
            - ef2f51f7
  action: ALLOW
````
## Authorisation Policy for metrics "/q" - stopped working when locked down the service
````
kind: AuthorizationPolicy
apiVersion: security.istio.io/v1beta1
metadata:
  name: membersmetrics
  namespace: membersapp-mesh
spec:
  selector:
    matchLabels:
      app: members
  rules:
    - to:
        - operation:
            methods:
              - GET
            paths:
              - /q/*
  action: ALLOW


``````
# Integration with 3scale

## create a new service to isolate from normal demo

````
kind: Service
apiVersion: v1
metadata:
  name: threescalemembers
  namespace: membersapp
  labels:
    app: members
spec:
  ports:
    - name: http
      protocol: TCP
      port: 80
      targetPort: 8080
  selector:
    3scaleapp: members
````
## Create a gateway for Istio members to avoid it clashing with my normal 3scale demo

````
kind: Gateway
apiVersion: networking.istio.io/v1alpha3
metadata:
  name: members3scale-gateway
  namespace: membersapp-mesh
spec:
  servers:
    - hosts:
        - members3scale.apps.coffee.demolab.local
      port:
        name: http
        number: 80
        protocol: HTTP
  selector:
    istio: ingressgateway
````
### need to create service entries for 3scale backend and system endpoints

Backend:
````
kind: ServiceEntry
apiVersion: networking.istio.io/v1alpha3
metadata:
  name: threescale-backend
  namespace: membersapp-mesh
spec:
  hosts:
    - backend-listener.amp.svc.cluster.local
  ports:
    - name: http
      number: 3000
      protocol: HTTP
  location: MESH_EXTERNAL
  resolution: DNS
````
System:
````
kind: ServiceEntry
apiVersion: networking.istio.io/v1alpha3
metadata:
  name: threescale-system
  namespace: membersapp-mesh
spec:
  hosts:
    - system-provider.amp.svc.cluster.local
  ports:
    - name: http
      number: 3000
      protocol: HTTP
  location: MESH_EXTERNAL
  resolution: DNS

````

## added initial virtual service
````
kind: VirtualService
apiVersion: networking.istio.io/v1alpha3
metadata:
  name: members
  namespace: membersapp-mesh
spec:
  hosts:
    - members3scale.apps.coffee.demolab.local
  gateways:
    - membersapp-mesh/members3scale-gateway
  http:
    - match:
        - uri:
            exact: /v1/membersweb/rest/members
        - uri:
            prefix: /v1/membersweb/rest/members
      rewrite:
        uri: /membersweb/rest/members
      route:
        - destination:
            host: members.membersapp-mesh.svc.cluster.local
            subset: v1
          weight: 100
        - destination:
            host: members.membersapp-mesh.svc.cluster.local
            subset: v2
    - match:
        - uri:
            exact: /v2/membersweb/rest/members
        - uri:
            prefix: /v2/membersweb/rest/members
      rewrite:
        uri: /membersweb/rest/members
      route:
        - destination:
            host: members.membersapp-mesh.svc.cluster.local
            subset: v1
        - destination:
            host: members.membersapp-mesh.svc.cluster.local
            subset: v2
          weight: 100
    - match:
        - uri:
            exact: /q/metrics
      route:
        - destination:
            host: members.membersapp-mesh.svc.cluster.local
            subset: v1
          weight: 50
        - destination:
            host: members.membersapp-mesh.svc.cluster.local
            subset: v2
          weight: 50
````

## Service Mesh Extension

Get the backend authentication value for a given service, used below in the backend defintion (token)  :

### get the API backend token from 3scale
curl -v  -X GET "https://red-hat-admin.apps.coffee.demolab.local/admin/api/services/14/proxy/configs/production/latest.json?access_token=97208a746907b2317ec37d324be116585bc19486a020efb7c07c8efbe18b1142" -k |jq '.proxy_config.content.backend_authentication_value'

### Update tokens
Make sure the tokens are changed accordingly

backend token from above goes in spec.config.services.authorites.token
user generated token goes in spec.config.system.token

Make sure you change the spec.config.services.authorites.id to the id of the required 3scale product

````
apiVersion: maistra.io/v1
kind: ServiceMeshExtension
metadata:
  name: threescale-auth
  namespace: membersapp-mesh
spec:
  config:
    api: v1
    backend:
      extensions:
        - no_body
      name: backend
      upstream:
        name: outbound|3000||backend-listener.amp.svc.cluster.local
        timeout: 5000
        url: 'http://backend-listener.amp.svc.cluster.local'
    services:
      - authorities:
          - '*'
        credentials:
          app_id:
            - header:
                keys:
                  - app_id
            - query_string:
                keys:
                  - app_id
          app_key:
            - header:
                keys:
                  - app_key
            - query_string:
                keys:
                  - app_key
          user_key:
            - query_string:
                keys:
                  - user_key
            - header:
                keys:
                  - user_key
        id: '14'
        mapping_rules:
          - method: GET
            pattern: /membersweb/rest/members$
            usages:
              - delta: 1
                name: allmembers
          - method: GET
            pattern: '/membersweb/rest/members/{id}$'
            usages:
              - delta: 1
                name: allmembersbyid
          - method: POST
            pattern: /membersweb/rest/members
            usages:
              - delta: 1
                name: add_member
          - method: DELETE
            pattern: '/membersweb/rest/members/{email}$'
            usages:
              - delta: 1
                name: deletemember
        token: 8155941048327d1e0fcc0b43b5d89813a343721b690e0ba29c38fd7db7622d48
    system:
      name: system
      token: 814f45259bde5ceded780fa82858c3c27865d3cac8842652adba1712d3ddbf39
      upstream:
        name: outbound|3000||system-provider.amp.svc.cluster.local
        timeout: 5000
        url: 'http://system-provider.amp.svc.cluster.local'
  image: 'registry.redhat.io/openshift-service-mesh/3scale-auth-wasm-rhel8:0.0.1'
  phase: PostAuthZ
  priority: 100
  workloadSelector:
    labels:
      app: members
````