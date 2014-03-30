
public class ServerRepository extends Repository {
	
	public ServerRepository(String user) throws Exception{
		super(user);
	}

	/* (non-Javadoc)
	 * @see Repository#addCommit(Commit)
	 */
	@Override
	public void addCommit(Commit c) {
		//hier moeten checks voor integriteit gebeuren
	}

}
