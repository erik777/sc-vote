In this folder, place your tls.crt and tls.key.  


***  
**The production tls.key should not be placed into source control!  It should be kept private.**
***  

You then create the certificate for use by Ingress by creating the secret:

    kubectl create secret tls sc-vote-sandbox-secret --key tls.key --cert tls.crt
    
    