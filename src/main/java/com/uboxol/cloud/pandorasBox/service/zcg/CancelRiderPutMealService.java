package com.uboxol.cloud.pandorasBox.service.zcg;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.druid.util.StringUtils;
import com.uboxol.cloud.pandorasBox.api.req.ClearGrids;
import com.uboxol.cloud.pandorasBox.api.req.Grid;
import com.uboxol.cloud.pandorasBox.api.req.RiderPutMeal;
import com.uboxol.cloud.pandorasBox.api.res.ClearResult;
import com.uboxol.cloud.pandorasBox.api.res.EntryResult;
import com.uboxol.cloud.pandorasBox.api.res.OrderInformation;
import com.uboxol.cloud.pandorasBox.db.entity.zcg.Counter;
import com.uboxol.cloud.pandorasBox.db.entity.zcg.Order;
import com.uboxol.cloud.pandorasBox.db.entity.zcg.OrderRec;
import com.uboxol.cloud.pandorasBox.db.repository.zcg.CounterRepository;
import com.uboxol.cloud.pandorasBox.db.repository.zcg.OrderRecRepository;
import com.uboxol.cloud.pandorasBox.db.repository.zcg.OrderRepository;

import lombok.extern.slf4j.Slf4j;

/*
 * 订单取消
业务描述：当骑手通过【2骑手放餐接口】开门后，业务端提供“确认放餐”按钮，新增“取消放餐”按钮
需求描述：为支持取消放餐按钮，需新增【11骑手取消放餐接口】，数据库新增字段“订单取消时间”
                 【11骑手取消放餐接口】
输入：业务方id，柜子id，格子id，取消放餐
输出：订单id，业务方id，柜子id，格子id，
           格子状态修改为：空闲中，
           订单状态修改为：订单取消，
           订单取消时间
*/

@Slf4j
@Service
public class CancelRiderPutMealService {
	private final CounterRepository counterRepository;
	private final OrderRepository orderRepository; 
	private final OrderRecRepository orderRecRepository; 
	
	@Autowired 
	 public CancelRiderPutMealService(final CounterRepository counterRepository,final OrderRepository orderRepository,final OrderRecRepository orderRecRepository) {
	 	 this.counterRepository = counterRepository;
		 this.orderRepository = orderRepository;
		 this.orderRecRepository = orderRecRepository;
	}
	
	
	public EntryResult cancel(Grid req) { 
		EntryResult entryResult = new EntryResult();
		try {
			String bussinessId = req.getBussinessId();
			String cabinetId = req.getCabinetId();
			String gridId = req.getGridId();
			
			Counter counter = counterRepository.findByCabinetIdAndGridId(cabinetId,gridId);
			if(counter==null) throw new RuntimeException("counter表中没有对应的格子");
			counter.setGridCurStatus(3);
			counter.setOrderCurStatus(5);
			counter.setCurTime(new Timestamp(new Date().getTime()));
			counterRepository.saveAndFlush(counter);
			logger.info("柜子："+cabinetId+"格口"+gridId+"更新格子状态为空闲，订单状态为取消");
			
			Order order =orderRepository.findByOrderId(counter.getCurOrderId());
			if(order==null) throw new RuntimeException("order表中没有对应的订单");
			order.setGridStatus(3);
			order.setOrderStatus(5);
			order.setOrderCancelTime(new Timestamp(new Date().getTime()));
			orderRepository.saveAndFlush(order);
			logger.info("更新订单："+counter.getCurOrderId()+"的格子状态为空闲，订单状态为取消");
			
			OrderRec orderRec = new OrderRec();
			orderRec.setOrderId(counter.getCurOrderId());
			orderRec.setBussinessId(bussinessId);
			orderRec.setCabinetId(counter.getCabinetId());
			orderRec.setGridId(counter.getGridId());
			orderRec.setSpecs(counter.getSpecs());
			orderRec.setBranchCompany(counter.getBranchCompany());
			orderRec.setPointId(counter.getPointId());
			orderRec.setPointName(counter.getPointName());
			orderRec.setOrderReason("骑手取消放餐");
			orderRec.setCleanStatus(0);
			orderRec.setGridStatus(3);
			orderRec.setOrderStatus(5);
			orderRec.setOrderClosedTime(new Timestamp(new Date().getTime()));
			orderRecRepository.saveAndFlush(orderRec);
			logger.info("orderRec记录表新增完毕");
			
			
			OrderInformation orderInformation = new OrderInformation();
			orderInformation.setOrderId(order.getOrderId());
			orderInformation.setBussinessId(order.getBussinessId());
			orderInformation.setCabinetId(order.getCabinetId());
			orderInformation.setGridId(order.getGridId());
			orderInformation.setGridStatus(order.getGridStatus());
			orderInformation.setOrderStatus(order.getOrderStatus());
			orderInformation.setOrderCancelTime(order.getOrderCancelTime());
			
			entryResult.setCode("200");
			entryResult.setMsg("骑手取消放餐成功");
			entryResult.setOrderInformation(orderInformation);
			
		}catch (Exception e) {
			logger.error("骑手取消放餐接口出错:{}", e.getMessage(), e); 
		}
		return entryResult;
	}
	
}