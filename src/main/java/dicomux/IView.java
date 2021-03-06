package dicomux;

/**
 * an interface for all views of Dicomux
 * @author heidi
 *
 */
public interface IView {
	/**
	 * the model calls that method in order to inform the view that new data is available
	 */
	public void notifyView();
	
	/**
	 * registers a model in the view<br/>
	 * the view will get its data only from that model
	 * @param model the model which shoul be registered
	 */
	public void registerModel(IModel model);
	
	/**
	 * registers a controller in the view<br/>
	 * the view will use the controller as EventHandler
	 * @param controller
	 */
	public void registerController(IController controller);
	
	/**
	 * it is possible that the returned value is -1
	 * @return the id of the currently active workspace
	 */
	public int getActiveWorkspaceId();	
}
