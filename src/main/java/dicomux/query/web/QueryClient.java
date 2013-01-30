package dicomux.query.web;

import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;

public class QueryClient {
	private String pacsId;
	private LinkedList<HttpClient> clients = new LinkedList<HttpClient>();

	public QueryClient(String pacsId) {
		this.pacsId = pacsId;
	}
    
    public QueryClient(String pacsId, URL baseUrl) {
    	this(pacsId);
    	addUrl(baseUrl);
    }
    
    public void addUrl(URL baseUrl) {
   		this.clients.add(new HttpClient(baseUrl));
    }

    //-- 
    
    //find-ecg?pacs=getest&patientId=164956&patientName=toto&dateRange=xxx
    public QueryResult findECG(String patientId, String patientName, String dateRange) throws IOException {
        return call("find-ecg", 
        		"patientId", patientId, 
        		"patientName", patientName, 
        		"dateRange", dateRange);
    }
    
    //--
    
    /*
     * Appel général:
     * - soit avec méthodes et paramètres séparés et un objet en retour (cas normal)
     * - soit avec méthodes et parametres en une seule chaine, et retour d'un flux (cas des pages "proxy" pour appel en js)
     * 
     * Le fonctionement est identique dans les deux cas :
     * 1. Tentative d'appel avec le premier serveur de la liste. Non synchronisé.
     * 2. En cas d'erreur, appel synchronisé d'une fonction particuliere
     * 2.1 retente le premier serveur de la liste, au cas ou une rotation aurait déjà eu lieu
     * 2.2 en cas d'echec, et tant que tous les clients n'ont pas été testés, on met place le dernier client testé en fin de liste
     * 2.3 si aucun client n'a fonctionné, on rejette l'erreur initiale. Sinon, on renvoie le résultat.
     * 
     * Au final, ca permet de toujours aller sur le premier client, qu'on déplace s'il part en erreur.
     */

    public QueryResult call(String method, String ... params) throws IOException {  
    	params = addPacsParameter(params);
    	
    	try {
    		HttpClient client = clients.getFirst();
    		return client.call(method, params);    			
    	} catch(IOException e) {    			  			    			
    		synchronized(clients) {
    			return callAfterError(e, method, params);
    		}
    	}
    }
    
    public InputStream callDirect(String methodAndParams) throws IOException {
    	methodAndParams = addPacsParameter(methodAndParams);
    	
    	try {
    		HttpClient client = clients.getFirst();
    		return client.callDirect(methodAndParams);    			
    	} catch(IOException e) {    			  			    			
    		synchronized(clients) {
    			return callDirectAfterError(e, methodAndParams);
    		}
    	}
    }
    
    //-- ajout du parametre pacs si non fourni
    
    private String[] addPacsParameter(String[] params) {
    	//renvoie tel quel si le paramtetre "pacs" est déjà présent
    	for(int i=0;i<params.length;i+=2) {
    		if("pacs".equals(params[i]))
    			return params;
    	}
    	
    	//rajoute le parametre sinon
    	String[] paramsWithPacs = new String[params.length + 2];
    	paramsWithPacs[0] = "pacs";
    	paramsWithPacs[1] = pacsId;
    	System.arraycopy(params, 0, paramsWithPacs, 2, params.length);
    	return paramsWithPacs;
    }
    
    private String addPacsParameter(String methodAndParams) {
    	//renvoie tel quel si le paramtetre "pacs" est déjà présent
    	if(methodAndParams.contains("?pacs=") || methodAndParams.contains("&pacs="))
    		return methodAndParams;
    	
    	if(methodAndParams.contains("?"))
    		return methodAndParams + "&pacs=" + pacsId;

    	return methodAndParams + "?pacs=" + pacsId;
    }
    
    
    //-- cas d'erreur, voir descriptif plus haut.

    private QueryResult callAfterError(IOException error, String method, String ... params) throws IOException {
    	for(int i=0;i<clients.size();i++) {
    		HttpClient client = clients.getFirst();
    		try {
    			return client.call(method, params);    			
    		} catch(IOException e) {    			  			    			
    			//rotate and try again, using the next client
    			Logger.getLogger(QueryClient.class).warn("Client failed, rotating client list:  <" + client + "> " + e);
    			Collections.rotate(clients, -1);
    		}
    	}

    	throw error;
    }

    private InputStream callDirectAfterError(IOException error, String methodAndParams) throws IOException {
    	for(int i=0;i<clients.size();i++) {
    		HttpClient client = clients.getFirst();
    		try {
    			return client.callDirect(methodAndParams);    	 			
    		} catch(IOException e) {    			  			    			
    			//rotate and try again, using the next client
    			Logger.getLogger(QueryClient.class).warn("Client failed, rotating client list:  <" + client + "> " + e);
    			Collections.rotate(clients, -1);
    		}
    	}
    		
    	throw error;    	
    }    
}
