package com.famoco.kyctelcomr.core.repositories

import android.content.Context
import android.util.Log
import com.famoco.kyctelcomrtlib.PeripheralAccess
import dagger.hilt.android.qualifiers.ApplicationContext
import org.ksoap2.SoapEnvelope
import org.ksoap2.serialization.SoapObject
import org.ksoap2.serialization.SoapSerializationEnvelope
import org.ksoap2.transport.HttpTransportSE
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.StringReader
import javax.inject.Inject
import javax.inject.Singleton
import javax.xml.parsers.DocumentBuilderFactory

@Singleton
class NetworkRepository @Inject constructor(@ApplicationContext private val context: Context,private val peripheralAccess: PeripheralAccess){

    companion object {
        private val TAG = NetworkRepository::class.java.simpleName
    }

    // Elements of a SOAP envelop body.
    private val NAMESPACE = "http://crm.authent.mattel"
    private val METHOD_NAME = "authentifierSIM"


    // SOAP Action header field URI consisting of the namespace and method that's used to make a
    // call to the web service.
    private val SOAP_ACTION = NAMESPACE + "/" + METHOD_NAME

    // Web service URL (that should be openable) along with the Web Service Definition Language
    // (WSDL) that's used to view the WSDL file by simply adding "?WSDL" to the end of the URL.
    private val URL = "http://41.223.99.84:8283/authentiCRM/services/AuthentiCRM?wsdl"

    fun sendIdentity(msisdn:String,imsi:String): Int {

        val request = SoapObject(NAMESPACE, METHOD_NAME)

        val cardNumber = peripheralAccess.cardNumber.value
        val personalNumber = peripheralAccess.identity.value?.personalNumber
        val  prenom = peripheralAccess.identity.value?.firstName
        val  nom = peripheralAccess.identity.value?.lastName
        val sexe=peripheralAccess.identity.value?.sex
        val date_naiss=peripheralAccess.identity.value?.dateOfBirth
        val lieu_naissance=peripheralAccess.identity.value?.placeOfBirth


        // The following adds a parameter (parameter name, user inputted value).
       // request.addProperty("CountryName", "France")
        request.addProperty("msisdn", msisdn)
        request.addProperty("imsi", imsi)
        request.addProperty("nni", personalNumber)
        request.addProperty("code_piece", 1)
        request.addProperty("nom", nom)
        request.addProperty("prenom", prenom)
        request.addProperty("sexe", sexe)
        request.addProperty("date_naiss", date_naiss)
        request.addProperty("lieu_naissance", lieu_naissance)
        request.addProperty("id_appareil", "0")
        request.addProperty("localisation", "0")
        request.addProperty("type_operation", 1)

        // Declares the version of the SOAP request.
        val envelope = SoapSerializationEnvelope(SoapEnvelope.VER12)

        // Set the following variable to true for compatibility with what seems to be the
        // default encoding for .Net-Services.
        envelope.dotNet = true

        // Assigns the SoapObject to the envelope as the outbound message for the SOAP call.
        envelope.setOutputSoapObject(request)

        // A J2SE based HttpTransport layer instantiation of the web service URL and the
        // WSDL file.
        val httpTransport = HttpTransportSE(URL)
        try {

            // This is the actual part that will call the webservice by setting the desired
            // header SOAP Action header field.
            httpTransport.call(SOAP_ACTION, envelope)

            // Returns a list of data (cities) after extracting data from the XML response.
            //return extractDataFromXmlResponse(envelope)
            return  envelope.response.toString().toInt();

            //Integer.parse(InputSource(StringReader(envelope.response.toString())))

        } catch (e: Exception) { // Many kinds of exceptions can be caught here
            Log.e(TAG, e.toString())
        }

        // Otherwise, returns null.
        return 0
    }

    @Throws(Exception::class)
    private fun extractDataFromXmlResponse(envelope: SoapSerializationEnvelope): List<String> {

        // Initializes a list to add elements (cities).
        val citiesList = mutableListOf<String>()

        // Initializes/instantiates a DocumentBuilder to parse the response from the SOAP envelope
        // in order to build an XML object, or a Document in this case.
        val docBuildFactory = DocumentBuilderFactory.newInstance()
        val docBuilder = docBuildFactory.newDocumentBuilder()
        val doc = docBuilder.parse(InputSource(StringReader(envelope.response.toString())))

        // Retrieves a list of Table nodes from the Document in order to iterate through.
        val nodeList = doc.getElementsByTagName("Table")
        for (i in 0 until nodeList.length) {

            // Retrieves each Table node.
            val node = nodeList.item(i)

            // Runs the following functionality should the node be of an element type.
            if (node.nodeType == Node.ELEMENT_NODE) {

                // Initially casts the node as an element.
                val element = node as Element

                // Adds each city to the list.
                citiesList.add(element.getElementsByTagName("City").item(0).textContent)
            }
        }
        return citiesList
    }
}