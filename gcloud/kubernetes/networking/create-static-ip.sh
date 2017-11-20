echo creating static IP 'sc-vote-ip' to be used by ingress

# gcloud compute addresses create sc-vote-ip --region=us-central1
gcloud compute addresses create sc-vote-ip --global

gcloud compute addresses list

echo to delete: gcloud compute addresses delete sc-vote-ip --global
