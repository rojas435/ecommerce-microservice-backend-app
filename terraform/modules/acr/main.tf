# ACR Module - Azure Container Registry
# Basic SKU for cost optimization (~$5/month)

resource "azurerm_container_registry" "main" {
  name                = var.registry_name
  resource_group_name = var.resource_group_name
  location            = var.location
  sku                 = var.sku
  admin_enabled       = true # Required for Kubernetes integration

  tags = merge(
    var.tags,
    {
      Environment = var.environment
      ManagedBy   = "Terraform"
    }
  )
}

# Note: Role assignment must be done manually or by service principal with User Access Administrator role
# Run this after deployment:
# az aks update --resource-group <rg-name> --name <cluster-name> --attach-acr <acr-name>
