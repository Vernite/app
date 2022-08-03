package com.workflow.workflow.integration.git;

/**
 * Object representing general git pull request. Its common interface for
 * various git services.
 */
public class PullRequest extends Issue {
    private String branch;

    public PullRequest() {
        super();
    }

    public PullRequest(long id, String url, String title, String description, String service, String branch) {
        super(id, url, title, description, service);
        this.branch = branch;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }
}
