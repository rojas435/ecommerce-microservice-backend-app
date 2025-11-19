# Backend configuration for Terraform state
# Stores state in Azure Storage Account for remote access, locking, and versioning

terraform {
  backend "azurerm" {
    resource_group_name  = "rg-terraform-state"
    storage_account_name = "tfstaterojas435"
    container_name       = "tfstate"
    key                  = "dev.terraform.tfstate"
  }
}
