package com.selimhorri.app.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.id.OrderItemId;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.OrderItemDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.exception.wrapper.OrderItemNotFoundException;
import com.selimhorri.app.helper.OrderItemMappingHelper;
import com.selimhorri.app.repository.OrderItemRepository;
import com.selimhorri.app.service.OrderItemService;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class OrderItemServiceImpl implements OrderItemService {
	
	private final OrderItemRepository orderItemRepository;
	private final RestTemplate restTemplate;
    
	@Value("${features.enrich-order-item-details:true}")
	private boolean enrichOrderItemDetails;
	
	@Override
	@CircuitBreaker(name = "shippingService")
	@Retry(name = "shippingService")
	@Bulkhead(name = "shippingService")
	public List<OrderItemDto> findAll() {
		log.info("*** OrderItemDto List, service; fetch all orderItems *");
		return this.orderItemRepository.findAll()
				.stream()
					.map(OrderItemMappingHelper::map)
					.map(o -> {
						if (enrichOrderItemDetails) {
							o.setProductDto(fetchProduct(o.getProductDto().getProductId()));
							o.setOrderDto(fetchOrder(o.getOrderDto().getOrderId()));
						}
						return o;
					})
					.distinct()
					.collect(Collectors.toUnmodifiableList());
	}
	
	@Override
	@CircuitBreaker(name = "shippingService")
	@Retry(name = "shippingService")
	@Bulkhead(name = "shippingService")
	public OrderItemDto findById(final OrderItemId orderItemId) {
		log.info("*** OrderItemDto, service; fetch orderItem by id *");
		return this.orderItemRepository.findById(orderItemId)
				.map(OrderItemMappingHelper::map)
				.map(o -> {
					if (enrichOrderItemDetails) {
						o.setProductDto(fetchProduct(o.getProductDto().getProductId()));
						o.setOrderDto(fetchOrder(o.getOrderDto().getOrderId()));
					}
					return o;
				})
				.orElseThrow(() -> new OrderItemNotFoundException(String.format("OrderItem with id: %s not found", orderItemId)));
	}
	
	@Override
	public OrderItemDto save(final OrderItemDto orderItemDto) {
		log.info("*** OrderItemDto, service; save orderItem *");
		return OrderItemMappingHelper.map(this.orderItemRepository
				.save(OrderItemMappingHelper.map(orderItemDto)));
	}
	
	@Override
	public OrderItemDto update(final OrderItemDto orderItemDto) {
		log.info("*** OrderItemDto, service; update orderItem *");
		return OrderItemMappingHelper.map(this.orderItemRepository
				.save(OrderItemMappingHelper.map(orderItemDto)));
	}
	
	@Override
	public void deleteById(final OrderItemId orderItemId) {
		log.info("*** Void, service; delete orderItem by id *");
		this.orderItemRepository.deleteById(orderItemId);
	}

    @CircuitBreaker(name = "shippingService", fallbackMethod = "fallbackProduct")
    @Retry(name = "shippingService")
    @Bulkhead(name = "shippingService")
    private ProductDto fetchProduct(Integer productId) {
        return this.restTemplate.getForObject(
            AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/" + productId,
            ProductDto.class
        );
    }

    private ProductDto fallbackProduct(Integer productId, Throwable t) {
        log.warn("Product service fallback for id={}, reason={}", productId, t.toString());
        return ProductDto.builder().productId(productId).build();
    }

    @CircuitBreaker(name = "shippingService", fallbackMethod = "fallbackOrder")
    @Retry(name = "shippingService")
    @Bulkhead(name = "shippingService")
    private OrderDto fetchOrder(Integer orderId) {
        return this.restTemplate.getForObject(
            AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/" + orderId,
            OrderDto.class
        );
    }

    private OrderDto fallbackOrder(Integer orderId, Throwable t) {
        log.warn("Order service fallback for id={}, reason={}", orderId, t.toString());
        return OrderDto.builder().orderId(orderId).build();
    }

}
















