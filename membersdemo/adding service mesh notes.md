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
  uid: d29a0f02-2e22-46a3-96cc-f1ced7dbc4ac
  resourceVersion: '1704598'
  generation: 2
  creationTimestamp: '2022-03-31T10:31:35Z'
  managedFields:
    - manager: Mozilla
      operation: Update
      apiVersion: networking.istio.io/v1beta1
      time: '2022-03-31T10:31:35Z'
      fieldsType: FieldsV1
      fieldsV1:
        'f:spec':
          .: {}
          'f:hosts': {}
          'f:http': {}
spec:
  hosts:
    - membersdemov1.membersapp.svc.cluster.local
  http:
    - match:
        - uri:
            prefix: membersweb
      name: members-v1-routes
      route:
        - destination:
            host: membersdemov1.membersapp.svc.cluster.local
````

## deployment type

quarkus.openshift.deployment-kind=Deployment