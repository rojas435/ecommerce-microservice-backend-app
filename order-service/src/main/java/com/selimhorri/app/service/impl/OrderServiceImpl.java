package com.selimhorri.app.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.exception.wrapper.OrderNotFoundException;
import com.selimhorri.app.helper.OrderMappingHelper;
import com.selimhorri.app.metrics.OrderMetricsRecorder;
import com.selimhorri.app.repository.OrderRepository;
import com.selimhorri.app.service.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
	
	private final OrderRepository orderRepository;
	private final OrderMetricsRecorder orderMetricsRecorder;
	
	@Override
	public List<OrderDto> findAll() {
		log.info("*** OrderDto List, service; fetch all orders *");
		return this.orderRepository.findAll()
				.stream()
					.map(OrderMappingHelper::map)
					.distinct()
					.collect(Collectors.toUnmodifiableList());
	}
	
	@Override
	public OrderDto findById(final Integer orderId) {
		log.info("*** OrderDto, service; fetch order by id *");
		return this.orderRepository.findById(orderId)
				.map(OrderMappingHelper::map)
				.orElseThrow(() -> new OrderNotFoundException(String
						.format("Order with id: %d not found", orderId)));
	}
	
	@Override
	public OrderDto save(final OrderDto orderDto) {
		log.info("*** OrderDto, service; save order *");
		final OrderDto saved = OrderMappingHelper.map(this.orderRepository
				.save(OrderMappingHelper.map(orderDto)));
		this.orderMetricsRecorder.recordOrderCreated(saved);
		return saved;
	}
	
	@Override
	public OrderDto update(final OrderDto orderDto) {
		log.info("*** OrderDto, service; update order *");
		final OrderDto updated = OrderMappingHelper.map(this.orderRepository
				.save(OrderMappingHelper.map(orderDto)));
		this.orderMetricsRecorder.recordOrderUpdated(updated);
		return updated;
	}
	
	@Override
	public OrderDto update(final Integer orderId, final OrderDto orderDto) {
		log.info("*** OrderDto, service; update order with orderId *");
		final OrderDto updated = OrderMappingHelper.map(this.orderRepository
				.save(OrderMappingHelper.map(this.findById(orderId))));
		this.orderMetricsRecorder.recordOrderUpdated(updated);
		return updated;
	}
	
	@Override
	public void deleteById(final Integer orderId) {
		log.info("*** Void, service; delete order by id *");
		this.orderRepository.delete(OrderMappingHelper.map(this.findById(orderId)));
		this.orderMetricsRecorder.recordOrderDeleted();
	}
	
	
	
}










