package com.cericlabs.jcnlib.util.preprocessor;

import com.cericlabs.jcnlib.CNConnection;


public interface MessagePreprocessor {

	public String processInboundMessage(CNConnection connection, String message);

	public String processOutboundMessage(CNConnection connection, String message);

}