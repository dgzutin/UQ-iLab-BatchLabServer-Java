/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uq.ilabs.labserver.service;

import edu.mit.ilab.AuthHeader;
import edu.mit.ilab.ObjectFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import javax.xml.soap.*;
import javax.xml.ws.ProtocolException;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import uq.ilabs.library.lab.utilities.Logfile;
import uq.ilabs.library.labserver.Consts;
import uq.ilabs.library.labserver.engine.ConfigProperties;
import uq.ilabs.library.labserver.engine.LabConsts;

/**
 *
 * @author uqlpayne
 */
public class ServiceBrokerAuthenticator implements SOAPHandler<SOAPMessageContext> {

    //<editor-fold defaultstate="collapsed" desc="Constants">
    private static final String STR_ClassName = ServiceBrokerAuthenticator.class.getName();
    /*
     * String constants for authorization header
     */
    public static final String STR_Identifier = "identifier";
    public static final String STR_Passkey = "passkey";
    /*
     * String constants for logfile messages
     */
    private static final String STRLOG_LoggingLevel_arg = "LoggingLevel: %s";
    //</editor-fold>

    @Override
    public boolean handleMessage(SOAPMessageContext messageContext) {
        /*
         * Assume this will fail
         */
        boolean success = false;

        /*
         * Process the header info for an inbound message if authentication is required
         */
        if ((Boolean) messageContext.get(SOAPMessageContext.MESSAGE_OUTBOUND_PROPERTY) == false) {
            try {
                /*
                 * Check if initialisation parameters have been read from the web.xml file
                 */
                if (LabServerService.isInitialised() == false) {
                    GetInitParameters((ServletContext) messageContext.get(MessageContext.SERVLET_CONTEXT));
                }

                /*
                 * Write the SOAP message to system output
                 */
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                messageContext.getMessage().writeTo(outputStream);
//                System.out.println(outputStream.toString());

                /*
                 * Process the SOAP header to get the authentication information
                 */
                ProcessSoapHeader(messageContext);

                success = true;
            } catch (SOAPException | IOException ex) {
                Logfile.WriteError(ex.toString());
                throw new ProtocolException(ex);
            }
        }

        return success;
    }

    @Override
    public Set<QName> getHeaders() {
        return Collections.emptySet();
    }

    @Override
    public boolean handleFault(SOAPMessageContext messageContext) {
        return false;
    }

    @Override
    public void close(MessageContext context) {
    }

    /**
     *
     * @param messageContext
     * @throws SOAPException
     */
    private void ProcessSoapHeader(SOAPMessageContext messageContext) throws SOAPException {
        /*
         * Get the SOAP header
         */
        SOAPMessage soapMessage = messageContext.getMessage();
        SOAPEnvelope soapEnvelope = soapMessage.getSOAPPart().getEnvelope();
        SOAPHeader soapHeader = soapEnvelope.getHeader();

        /*
         * Get the authentication header names
         */
        ObjectFactory objectFactory = new ObjectFactory();
        JAXBElement<AuthHeader> jaxbElement = objectFactory.createAuthHeader(new AuthHeader());
        String qnameAuthHeaderLocalPart = jaxbElement.getName().getLocalPart();

        /*
         * Scan through the header's child elements looking for the authentication header
         */
        Iterator iterator = soapHeader.getChildElements();
        while (iterator.hasNext()) {
            /*
             * Get the next child looking for a SOAPElement type
             */
            Object object = iterator.next();
            if (object instanceof SOAPElement) {
                /*
                 * Get the SOAPElement looking for the authentication header
                 */
                SOAPElement soapElement = (SOAPElement) object;
                Name elementName = soapElement.getElementName();
                String localName = elementName.getLocalName();

                /*
                 * Check if this is an authentication header
                 */
                if (localName.equalsIgnoreCase(qnameAuthHeaderLocalPart) == true) {
                    /*
                     * Put the authentication header into the message context where it can be processed by the web
                     * service
                     */
                    AuthHeader authHeader = ProcessSoapElement(soapElement);
                    messageContext.put(qnameAuthHeaderLocalPart, authHeader);
                    messageContext.setScope(qnameAuthHeaderLocalPart, MessageContext.Scope.APPLICATION);
                }
            }
        }
    }

    /**
     *
     * @param soapElement
     * @return AuthHeader
     */
    private AuthHeader ProcessSoapElement(SOAPElement soapElement) {
        /*
         * Create an instance of the authentication header ready to fill in
         */
        ObjectFactory objectFactory = new ObjectFactory();
        AuthHeader authHeader = objectFactory.createAuthHeader();

        Iterator iterator = soapElement.getChildElements();
        while (iterator.hasNext()) {
            /*
             * Get the next child looking for a SOAPElement type
             */
            Object object = iterator.next();
            if (object instanceof SOAPElement) {
                /*
                 * Get the SOAPElement looking for the authentication information
                 */
                SOAPElement element = (SOAPElement) object;
                Name elementName = element.getElementName();
                String localName = elementName.getLocalName();

                /*
                 * Check if localName matches a specified string
                 */
                if (localName.equalsIgnoreCase(STR_Identifier) == true) {
                    authHeader.setIdentifier(element.getValue());
                } else if (localName.equalsIgnoreCase(STR_Passkey) == true) {
                    authHeader.setPassKey(element.getValue());
                }
            }
        }

        return authHeader;
    }

    /**
     *
     * @param servletContext
     */
    private void GetInitParameters(ServletContext servletContext) {
        final String methodName = "GetInitParameters";

        try {
            /*
             * Get the path for the logfiles and logging level
             */
            String logFilesPath = servletContext.getInitParameter(LabConsts.STRPRM_LogFilesPath);
            String logLevel = servletContext.getInitParameter(LabConsts.STRPRM_LogLevel);

            /*
             * Create an instance of the logger and set the logging level
             */
            Logger logger = Logfile.CreateLogger(logFilesPath);
            Level level;
            try {
                level = Level.parse(logLevel);
            } catch (Exception ex) {
                level = Level.INFO;
            }
            logger.setLevel(level);

            Logfile.WriteCalled(STR_ClassName, methodName,
                    String.format(STRLOG_LoggingLevel_arg, logger.getLevel().toString()));

            /*
             * Get configuration properties from the file
             */
            String xmlConfigPropertiesPath = servletContext.getInitParameter(LabConsts.STRPRM_XmlConfigPropertiesPath);
            ConfigProperties configProperties = new ConfigProperties(servletContext.getRealPath(xmlConfigPropertiesPath));
            LabServerService.setConfigProperties(configProperties);

            /*
             * Get the path to the XML LabConfiguration file
             */
            String xmlLabConfigurationPath = servletContext.getInitParameter(LabConsts.STRPRM_XmlLabConfigurationPath);
            configProperties.setXmlLabConfigurationPath(servletContext.getRealPath(xmlLabConfigurationPath));

            /*
             * Get the path to the XML SimulationConfig file
             */
            String xmlSimulationConfigPath = servletContext.getInitParameter(Consts.STRPRM_XmlSimulationConfigPath);
            configProperties.setXmlSimulationConfigPath(servletContext.getRealPath(xmlSimulationConfigPath));

            LabServerService.setInitialised(true);
        } catch (Exception ex) {
            Logfile.WriteError(ex.toString());
        }

        Logfile.WriteCompleted(STR_ClassName, methodName);
    }
}