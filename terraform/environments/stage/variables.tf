variable "project_name" {
  description = "Name of the project"
  type        = string
  default     = "ecommerce"
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "stage"
}

variable "location" {
  description = "Azure region"
  type        = string
  default     = "Chile Central"
}

variable "vnet_address_space" {
  description = "Address space for VNet"
  type        = string
  default     = "10.1.0.0/16"
}

variable "aks_subnet_address_prefix" {
  description = "Address prefix for AKS subnet"
  type        = string
  default     = "10.1.1.0/24"
}

variable "acr_name" {
  description = "Name of Azure Container Registry"
  type        = string
  default     = "ecommercerojas435stage"
}

variable "acr_sku" {
  description = "SKU for ACR"
  type        = string
  default     = "Basic"
}

variable "kubernetes_version" {
  description = "Kubernetes version"
  type        = string
  default     = "1.34.0"
}

variable "aks_node_count" {
  description = "Number of nodes in AKS"
  type        = number
  default     = 2 # HA testing
}

variable "aks_vm_size" {
  description = "VM size for AKS nodes"
  type        = string
  default     = "Standard_B2s"
}
