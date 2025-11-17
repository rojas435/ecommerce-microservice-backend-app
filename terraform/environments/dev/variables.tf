variable "project_name" {
  description = "Name of the project"
  type        = string
  default     = "ecommerce"
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "dev"
}

variable "location" {
  description = "Azure region"
  type        = string
  default     = "Chile Central"
}

# Networking Variables
variable "vnet_address_space" {
  description = "Address space for VNet"
  type        = string
  default     = "10.0.0.0/16"
}

variable "aks_subnet_address_prefix" {
  description = "Address prefix for AKS subnet"
  type        = string
  default     = "10.0.1.0/24"
}

# ACR Variables
variable "acr_name" {
  description = "Name of Azure Container Registry (globally unique)"
  type        = string
  default     = "ecommercerojas435acr" # Globally unique with your username
}

variable "acr_sku" {
  description = "SKU for ACR"
  type        = string
  default     = "Basic" # $5/month
}

# AKS Variables
variable "kubernetes_version" {
  description = "Kubernetes version"
  type        = string
  default     = "1.34.0"
}

variable "aks_node_count" {
  description = "Number of nodes in AKS"
  type        = number
  default     = 1 # Cost optimization
}

variable "aks_vm_size" {
  description = "VM size for AKS nodes"
  type        = string
  default     = "Standard_B2s" # 2 vCPUs, 4GB RAM
}
