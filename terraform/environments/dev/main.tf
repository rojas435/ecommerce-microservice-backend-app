# Development Environment Configuration
# Region: Chile Central
# Cost-optimized: 1 node, Basic ACR

terraform {
  required_version = ">= 1.5"
  
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.80"
    }
  }

  # Backend configuration for remote state
  # Run this AFTER creating storage account (see setup instructions)
  # backend "azurerm" {
  #   resource_group_name  = "terraform-state-rg"
  #   storage_account_name = "ecommercedevtfstate"
  #   container_name       = "tfstate"
  #   key                  = "dev.terraform.tfstate"
  # }
}

provider "azurerm" {
  features {}
  skip_provider_registration = true
}

# Resource Group
resource "azurerm_resource_group" "main" {
  name     = "${var.project_name}-${var.environment}-rg"
  location = var.location

  tags = {
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
    CreatedDate = timestamp()
  }
}

# Networking Module
module "networking" {
  source = "../../modules/networking"

  environment         = var.environment
  location            = var.location
  resource_group_name = azurerm_resource_group.main.name
  vnet_address_space  = var.vnet_address_space
  aks_subnet_address_prefix = var.aks_subnet_address_prefix

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

# AKS Module
module "aks" {
  source = "../../modules/aks"

  environment         = var.environment
  location            = var.location
  resource_group_name = azurerm_resource_group.main.name
  subnet_id           = module.networking.aks_subnet_id
  kubernetes_version  = var.kubernetes_version
  node_count          = var.aks_node_count
  vm_size             = var.aks_vm_size

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }

  depends_on = [module.networking]
}

# ACR Module
module "acr" {
  source = "../../modules/acr"

  environment         = var.environment
  location            = var.location
  resource_group_name = azurerm_resource_group.main.name
  registry_name       = var.acr_name
  sku                 = var.acr_sku

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }

  depends_on = [module.aks]
}
