package com.be.razorpay;

import com.be.razorpay.common.enums.PaymentMethod;
import com.be.razorpay.payment.gateway.PaymentAdapter;
import com.be.razorpay.payment.gateway.PaymentGatewayRouter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Map;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class RazorpayApplication {

    public static void main(String[] args) {
//		SpringApplication.run(RazorpayApplication.class, args);
		ApplicationContext context = SpringApplication.run(RazorpayApplication.class, args);
		Map<PaymentMethod, PaymentAdapter> map =
				context.getBean("paymentAdapterMap", Map.class);
		System.out.println("beasn"+map);
//		PaymentAdapter adapter = map.get(method);


	}

}
