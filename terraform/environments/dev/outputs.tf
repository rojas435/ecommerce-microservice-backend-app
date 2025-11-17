# Resource Group Outputs
output "resource_group_name" {
  description = "Name of the resource group"
  value       = azurerm_resource_group.main.name
}

# Networking Outputs
output "vnet_id" {
  description = "ID of the VNet"
  value       = module.networking.vnet_id
}

output "aks_subnet_id" {
  description = "ID of the AKS subnet"
  value       = module.networking.aks_subnet_id
}

# ACR Outputs
output "acr_login_server" {
  description = "ACR login server URL"
  value       = module.acr.login_server
}

output "acr_admin_username" {
  description = "ACR admin username"
  value       = module.acr.admin_username
  sensitive   = true
}

output "acr_admin_password" {
  description = "ACR admin password"
  value       = module.acr.admin_password
  sensitive   = true
}

# AKS Outputs
output "aks_cluster_name" {
  description = "Name of the AKS cluster"
  value       = module.aks.cluster_name
}

output "aks_cluster_fqdn" {
  description = "FQDN of the AKS cluster"
  value       = module.aks.cluster_fqdn
}

output "kube_config" {
  description = "Kubeconfig for the AKS cluster"
  value       = module.aks.kube_config_raw
  sensitive   = true
}

# Deployment Instructions
output "next_steps" {
  description = "Instructions for next steps after deployment"
  value       = <<-EOT
  
  ✅ Infrastructure deployed successfully!
  
  Next steps:
  
  1. Get AKS credentials:
     az aks get-credentials --resource-group ${azurerm_resource_group.main.name} --name ${module.aks.cluster_name}
  
  2. Verify cluster access:
     kubectl get nodes
  
  3. Login to ACR:
     az acr login --name ${var.acr_name}
  
  4. Get ACR credentials for Docker:
     terraform output -raw acr_admin_username
     terraform output -raw acr_admin_password
  
  5. Tag and push images to ACR:
     docker tag rojas43529/order-service:latest ${module.acr.login_server}/order-service:latest
     docker push ${module.acr.login_server}/order-service:latest
  
  6. Update Kubernetes manifests to use ACR images
  
  7. Deploy services to AKS:
     kubectl apply -f k8s/
  
  EOT
}
