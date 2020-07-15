package storage;

public class RepoLibrary {
    public String repoLink;
    public String commitID;
    public String libraryName;
    public boolean isAdded;
    public boolean isRemoved;
    public String pomPath;

    public RepoLibrary(final String repoLink, final String commitID, final String libraryName,
                       final boolean isAdded,
                       final boolean isRemoved,
                       final String pomPath) {
        this.repoLink = repoLink;
        this.commitID = commitID;
        this.libraryName = libraryName;
        this.isAdded = isAdded;
        this.isRemoved = isRemoved;
        this.pomPath = pomPath;
    }
}
