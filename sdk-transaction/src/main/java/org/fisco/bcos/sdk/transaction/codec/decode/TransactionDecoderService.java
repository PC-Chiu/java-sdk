/*
 * Copyright 2014-2020  [fisco-dev]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package org.fisco.bcos.sdk.transaction.codec.decode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.fisco.bcos.sdk.codec.ABICodec;
import org.fisco.bcos.sdk.codec.ABICodecException;
import org.fisco.bcos.sdk.codec.EventEncoder;
import org.fisco.bcos.sdk.codec.datatypes.Type;
import org.fisco.bcos.sdk.codec.wrapper.ABICodecObject;
import org.fisco.bcos.sdk.codec.wrapper.ABIDefinition;
import org.fisco.bcos.sdk.codec.wrapper.ABIDefinition.NamedType;
import org.fisco.bcos.sdk.codec.wrapper.ABIDefinitionFactory;
import org.fisco.bcos.sdk.codec.wrapper.ABIObject;
import org.fisco.bcos.sdk.codec.wrapper.ABIObjectFactory;
import org.fisco.bcos.sdk.codec.wrapper.ContractABIDefinition;
import org.fisco.bcos.sdk.crypto.CryptoSuite;
import org.fisco.bcos.sdk.model.RetCode;
import org.fisco.bcos.sdk.model.TransactionReceipt;
import org.fisco.bcos.sdk.model.TransactionReceipt.Logs;
import org.fisco.bcos.sdk.transaction.model.dto.TransactionResponse;
import org.fisco.bcos.sdk.transaction.model.exception.ContractException;
import org.fisco.bcos.sdk.transaction.model.exception.TransactionException;
import org.fisco.bcos.sdk.transaction.tools.JsonUtils;
import org.fisco.bcos.sdk.transaction.tools.ReceiptStatusUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionDecoderService implements TransactionDecoderInterface {
    protected static Logger logger = LoggerFactory.getLogger(TransactionDecoderService.class);

    private CryptoSuite cryptoSuite;
    private final ABICodec abiCodec;
    private final EventEncoder eventEncoder;

    /**
     * create TransactionDecoderService
     *
     * @param cryptoSuite the cryptoSuite used to calculate hash and signatures
     * @param isWasm whether the invoked contract is a Wasm contract
     */
    public TransactionDecoderService(CryptoSuite cryptoSuite, boolean isWasm) {
        super();
        this.cryptoSuite = cryptoSuite;
        this.abiCodec = new ABICodec(cryptoSuite, isWasm);
        this.eventEncoder = new EventEncoder(cryptoSuite);
    }

    @Override
    public String decodeReceiptMessage(String output) {
        return ReceiptStatusUtil.decodeReceiptMessage(output);
    }

    @Override
    public TransactionResponse decodeReceiptWithValues(
            String abi, String functionName, TransactionReceipt transactionReceipt)
            throws IOException, ABICodecException, TransactionException {
        TransactionResponse response = decodeReceiptWithoutValues(abi, transactionReceipt);
        // only successful tx has return values.
        if (transactionReceipt.getStatus() == 0) {
            List<Type> results =
                    abiCodec.decodeMethodAndGetOutputObject(
                            abi, functionName, transactionReceipt.getOutput());
            response.setResults(results);
        }
        return response;
    }

    @Override
    public TransactionResponse decodeReceiptWithoutValues(
            String abi, TransactionReceipt transactionReceipt)
            throws TransactionException, IOException, ABICodecException {
        TransactionResponse response = decodeReceiptStatus(transactionReceipt);
        response.setTransactionReceipt(transactionReceipt);
        response.setContractAddress(transactionReceipt.getContractAddress());
        // the exception transaction
        if (transactionReceipt.getStatus() != 0) {
            return response;
        }
        String events = JsonUtils.toJson(decodeEvents(abi, transactionReceipt.getLogEntries()));
        response.setEvents(events);
        return response;
    }

    @Override
    public TransactionResponse decodeReceiptStatus(TransactionReceipt receipt) {
        TransactionResponse response = new TransactionResponse();
        try {
            RetCode retCode = ReceiptParser.parseTransactionReceipt(receipt);
            response.setReturnCode(retCode.getCode());
            response.setReceiptMessages(retCode.getMessage());
            response.setReturnMessage(retCode.getMessage());
        } catch (ContractException e) {
            response.setReturnCode(e.getErrorCode());
            response.setReceiptMessages(e.getMessage());
            response.setReturnMessage(e.getMessage());
        }
        return response;
    }

    @Override
    public Map<String, List<List<Object>>> decodeEvents(String abi, List<Logs> logs)
            throws ABICodecException {
        ABIDefinitionFactory abiDefinitionFactory = new ABIDefinitionFactory(cryptoSuite);
        ContractABIDefinition contractABIDefinition = abiDefinitionFactory.loadABI(abi);
        Map<String, List<ABIDefinition>> eventsMap = contractABIDefinition.getEvents();
        Map<String, List<List<Object>>> result = new HashMap<>();
        if (logs == null) {
            return result;
        }
        eventsMap.forEach(
                (name, events) -> {
                    for (ABIDefinition abiDefinition : events) {
                        ABIObject outputObject =
                                ABIObjectFactory.createEventInputObject(abiDefinition);
                        ABICodecObject abiCodecObject = new ABICodecObject();
                        for (Logs log : logs) {
                            String eventSignature =
                                    eventEncoder.buildEventSignature(
                                            decodeMethodSign(abiDefinition));
                            if (log.getTopics().isEmpty()
                                    || !log.getTopics().contains(eventSignature)) {
                                continue;
                            }
                            try {
                                List<Object> list =
                                        abiCodecObject.decodeJavaObject(
                                                outputObject, log.getData());
                                if (result.containsKey(name)) {
                                    result.get(name).add(list);
                                } else {
                                    List<List<Object>> l = new ArrayList<>();
                                    l.add(list);
                                    result.put(name, l);
                                }
                            } catch (Exception e) {
                                logger.error(
                                        " exception in decodeEventToObject : {}", e.getMessage());
                            }
                        }
                    }
                });
        return result;
    }

    private String decodeMethodSign(ABIDefinition abiDefinition) {
        List<NamedType> inputTypes = abiDefinition.getInputs();
        StringBuilder methodSign = new StringBuilder();
        methodSign.append(abiDefinition.getName());
        methodSign.append("(");
        String params =
                inputTypes.stream().map(NamedType::getType).collect(Collectors.joining(","));
        methodSign.append(params);
        methodSign.append(")");
        return methodSign.toString();
    }

    /** @return the cryptoSuite */
    public CryptoSuite getCryptoSuite() {
        return cryptoSuite;
    }

    /** @param cryptoSuite the cryptoSuite to set */
    public void setCryptoSuite(CryptoSuite cryptoSuite) {
        this.cryptoSuite = cryptoSuite;
    }
}
