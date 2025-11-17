variable "environment" {
  description = "Environment name (dev, stage, prod)"
  type        = string
}

variable "location" {
  description = "Azure region for resources"
  type        = string
}

variable "resource_group_name" {
  description = "Name of the resource group"
  type        = string
}

variable "registry_name" {
  description = "Name of the container registry (must be globally unique, alphanumeric only)"
  type        = string
  validation {
    condition     = can(regex("^[a-zA-Z0-9]+$", var.registry_name))
    error_message = "Registry name must contain only alphanumeric characters."
  }
}

variable "sku" {
  description = "SKU for the container registry"
  type        = string
  default     = "Basic"
  validation {
    condition     = contains(["Basic", "Standard", "Premium"], var.sku)
    error_message = "SKU must be Basic, Standard, or Premium."
  }
}

variable "tags" {
  description = "Tags to apply to resources"
  type        = map(string)
  default     = {}
}
