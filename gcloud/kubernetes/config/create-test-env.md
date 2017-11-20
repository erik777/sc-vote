This assumes you activated the config with the project and data center settins you want to create this in.  

## Create a cluster

This uses the [`gcloud container clusters create`](https://cloud.google.com/sdk/gcloud/reference/container/clusters/create) command.  

	gcloud container clusters create sc-vote-1
	
	Creating cluster sc-vote-1...done.                                                                                                   
	Created [https://container.googleapis.com/v1/projects/sc-vote-001/zones/us-central1-c/clusters/sc-vote-1].
	kubeconfig entry generated for sc-vote-1.
	NAME       ZONE           MASTER_VERSION  MASTER_IP       MACHINE_TYPE   NODE_VERSION  NUM_NODES  STATUS
	sc-vote-1  us-central1-c  1.6.7           35.188.174.235  n1-standard-1  1.6.7         3          RUNNING
	
Delve deeper into setup

	$ gcloud container node-pools list --cluster=sc-vote-1
	
	NAME          MACHINE_TYPE   DISK_SIZE_GB  NODE_VERSION
	default-pool  n1-standard-1  100           1.6.7
	
	$ gcloud container node-pools describe default-pool --cluster=sc-vote-1
	
		config:
	  diskSizeGb: 100
	  imageType: COS
	  machineType: n1-standard-1
	  oauthScopes:
	  - https://www.googleapis.com/auth/compute
	  - https://www.googleapis.com/auth/devstorage.read_only
	  - https://www.googleapis.com/auth/service.management.readonly
	  - https://www.googleapis.com/auth/servicecontrol
	  - https://www.googleapis.com/auth/logging.write
	  - https://www.googleapis.com/auth/monitoring
	  serviceAccount: default
	initialNodeCount: 3
	instanceGroupUrls:
	- https://www.googleapis.com/compute/v1/projects/sc-vote-001/zones/us-central1-c/instanceGroupManagers/gke-sc-vote-1-default-pool-f32a1ace-grp
	management: {}
	name: default-pool
	selfLink: https://container.googleapis.com/v1/projects/sc-vote-001/zones/us-central1-c/clusters/sc-vote-1/nodePools/default-pool
	status: RUNNING
	version: 1.6.7
	
A bit more than what is needed for a test environment.  Let's drop and re-recreate with parameters.

	gcloud container clusters delete sc-vote-1 --quiet
	
We'll use a smaller [machine type](https://cloud.google.com/compute/docs/machine-types).

*********************
	
	gcloud container clusters create sc-vote-1 --machine-type=g1-small --num-nodes=1
	
*********************

Might need to come back and add a --scopes parameter for authorization.

Can add additional node pools with

	gcloud container node-pools create
	
To [resize the cluster](https://cloud.google.com/container-engine/docs/resize-cluster), if you only have one node pool, you can use

	gcloud container clusters resize CLUSTER_NAME --size SIZE
	
If you have multiple node pools, you need to specify the name

gcloud container clusters resize CLUSTER_NAME --node-pool NODE_POOL --size SIZE

## Deploy applications to new test environment

### Create the cluster

Or switch to it if not active.  

	gcloud container clusters create sc-vote-1 --machine-type=g1-small --num-nodes=1

Set `num-nodes` according to your requirements.  

### CrateDB

Check the number of nodes in your crate-statefulset*.yaml to be sure it is what you need for testing.  

Under kubernetes-cratedb/statefulset, create the storage class, statefulset and the service.  

	kubectl create -f crate-storage-class.yml
	kubectl create -f crate-statefulset-single.yml
	kubectl create -f crate-service.yml

### SC-Vote (Java microservice)

Create the DB schema.  In CloudCollector/doc/DESIGN.md, run the CREATE TABLE statement.  You can use the REST URL `scs/vote/v1/test/db` to programmatically create.

Compile with

	mvn  clean install
	
In `cloud/docker/build-gcr`, change the version number.

Create the docker and push to repository.  Run `build-gcr`.  

In order to push to the repository, you will need to be using the gcloud configuration of the user who can write to the repository.  After it runs successfully, switch back to the owner of the cluster you are deploying to.  When you switch back, you may need to get-credentials:

	gcloud container clusters get-credentials sc-vote-1 --zone us-central1-c --project sc-vote-001

Change version in `cloud/k8s/sc-vote-deployment.yaml` to match version in `build-gcr`.

Create the pods using YAML in `cloud/k8s`.

	kubectl create -f sc-vote-deployment.yaml
	kubectl create -f sc-vote-service.yaml

### sc-vote-ui

In `sc-vote-ui`, compile for production:

	npm run build:prod:aot
	
Open `cloud/docker/build-gcr` and increment the version number.  Run from command line to build docker and push to repository (switching users and setting credentials as needed). 
	
Update version number in `cloud/k8s/sc-vote-ui-deployment.yaml`.  
	
In `cloud/k8s`, run

	kubectl create -f sc-vote-ui-deployment.yaml
	kubectl create -f sc-vote-ui-service.yaml

### Ingress

In `gcloud/kubernetes/networking`, run:

	./create-static-ip.sh
	kubectl create -f sc-ingress.yaml
	
It can take awhile to propagate the rules, despite the Ingress being created, despite it returning the address after you query with `kubectl get ingress`.  You can get 404 errors until propagation completes.    
	
## Final script

### Create test environment

	gcloud container clusters create sc-vote-1 --machine-type=g1-small --num-nodes=2
	
* Deploy sc-vote-ui
	
### Validate environment

#### CrateDB

Create a port-forward rule and test in browser.  

#### SC Vote 

	kubectl port-forward POD_NAME 8090
	
Use these URLs:

	http://localhost:8090/
	http://localhost:8090/scs/vote/test
	http://localhost:8090/scs/vote/v1/test/db

### Destroy test environment

#### Delete the cluster

While technically not needed, it may be a good idea to get-credentials before deleting the cluster.  Otherwise, you may not be able to use `kubectl` to validate certain cleanup.  

	gcloud container clusters delete sc-vote-1 
	
#### Delete static ip

	gcloud compute addresses list
	gcloud compute addresses delete sc-vote-ip --global

#### Remove forwarding rules

Despite deleting Ingress or a cluster, Google can leave the forwarding rules behind.  

Use 

	gcloud compute forwarding-rules list

to identify.  Then use

	gcloud compute forwarding-rules describe NAME --region=REGION NAME
	gcloud compute forwarding-rules describe NAME --global

to verify it is one you want to remove.  Then use
	
	gcloud compute forwarding-rules delete NAME --region=REGION NAME

to remove. 

#### (Optional) Remove older repository versions


### Deploying locally



