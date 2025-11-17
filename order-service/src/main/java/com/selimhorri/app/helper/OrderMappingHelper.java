package com.selimhorri.app.helper;

import com.selimhorri.app.domain.Cart;
import com.selimhorri.app.domain.Order;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.OrderDto;

public interface OrderMappingHelper {
	
	public static OrderDto map(final Order order) {
		OrderDto.OrderDtoBuilder builder = OrderDto.builder()
				.orderId(order.getOrderId())
				.orderDate(order.getOrderDate())
				.orderDesc(order.getOrderDesc())
				.orderFee(order.getOrderFee());
		
		// Null-safe cart mapping
		if (order.getCart() != null) {
			builder.cartDto(CartDto.builder()
					.cartId(order.getCart().getCartId())
					.build());
		}
		
		return builder.build();
	}
	
	public static Order map(final OrderDto orderDto) {
		Order.OrderBuilder builder = Order.builder()
				.orderId(orderDto.getOrderId())
				.orderDate(orderDto.getOrderDate())
				.orderDesc(orderDto.getOrderDesc())
				.orderFee(orderDto.getOrderFee());
		
		// Null-safe cart mapping
		if (orderDto.getCartDto() != null) {
			builder.cart(Cart.builder()
					.cartId(orderDto.getCartDto().getCartId())
					.build());
		}
		
		return builder.build();
	}
	
	
	
}










