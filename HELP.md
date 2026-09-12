# General Resources for the project, commits, etc.

### Reference Documentation and Guides

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/4.1.1/maven-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/4.1.1/maven-plugin/build-image.html)
* [Spring Data JPA](https://docs.spring.io/spring-boot/4.1.1/reference/data/sql.html#data.sql.jpa-and-spring-data)
* [Spring Boot DevTools](https://docs.spring.io/spring-boot/4.1.1/reference/using/devtools.html)
* [OAuth2 Client](https://docs.spring.io/spring-boot/4.1.1/reference/web/spring-security.html#web.security.oauth2.client)
* [Spring Security](https://docs.spring.io/spring-boot/4.1.1/reference/web/spring-security.html)
* [Spring Web](https://docs.spring.io/spring-boot/4.1.1/reference/web/servlet.html)

* [Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa/)
* [Securing a Web Application](https://spring.io/guides/gs/securing-web/)
* [Spring Boot and OAuth2](https://spring.io/guides/tutorials/spring-boot-oauth2/)
* [Authenticating a User with LDAP](https://spring.io/guides/gs/authenticating-ldap/)
* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)

### Docker Compose support
* [Docker Compose Support](https://docs.spring.io/spring-boot/4.1.1/reference/features/dev-services.html#features.dev-services.docker-compose)
This project contains a Docker Compose file named `compose.yaml`.

However, no services were added yet. As of now, the application won't start!

### Commits Messages
For commits consider the following conventions:
* [Conventional Commits Summary](https://www.conventionalcommits.org/en/v1.0.0/#summary)

The general structure for commit messages considers a type and a description.

  <type>[optional scope]: <description>
  
  [optional body]
  
  [optional footer(s)]

There are several types of commit available such as build:, chore:, ci:, docs:, style:, refactor:, perf:, test:, etc. 
The most used ones are fix:, feat: and BREAKING CHANGE:. 
fix is used for bug patches in the codebase, feat introduces a new feature, and a BREAKING CHANGE footer (or a message with ! appended after the type/scope) introduces a breaking API change.

### Pull requests

* [Creating a Pull Request](https://docs.github.com/en/pull-requests/how-tos/create-pull-requests/creating-a-pull-request)
* [Linking a Pull Request to an Issue](https://docs.github.com/en/issues/tracking-your-work-with-issues/using-issues/linking-a-pull-request-to-an-issue)
* [Request a Review](https://docs.github.com/en/pull-requests/how-tos/create-pull-requests/requesting-a-pull-request-review)
* [Reviewing proposed changes](https://docs.github.com/en/pull-requests/how-tos/review-pull-requests/reviewing-proposed-changes-in-a-pull-request)

When the pull request targets the repository's default branch, a commit can be linked with the corresponding issue using the following keywords:
  close
  closes
  closed
  fix
  fixes
  fixed
  resolve
  resolves
  resolved

Examples:
  ***Issue in the same repository***:	Closes #10
  ***Issue in a different repository***:	Fixes octo-org/octo-repo#100
  ***Multiple issues***: Resolves #10, resolves #123, resolves octo-org/octo-repo#100

After creating a Pull Request you will need to request a review, you can do this by selecting the pull request you want a review on, select the reviewers and click on Request.
After the request is reviewed, you can ask for another review.

If you are requested to review a pull request, you can resolve the review by clicking Pull requests (you can find this under the repository name), then, select the pull request you want to review. There, click Files Changed and click on Review changes.
You can select one of three options: Comment, Approve or Request Changes. Select Comment if you want to give general feedback, Approve to submit feedback and approve the merging changes, and Request Changes if you want to suggest changes before giving the approval.
Finally, click on submit review (or discard review if you decide not to send it).

* Note: The rulesets established for this repository ask for two approvals before merging.

### Branches

* [Creating a branch to work on an issue](https://docs.github.com/en/issues/tracking-your-work-with-issues/using-issues/creating-a-branch-for-an-issue)

The main branch is protected, thus making changes requires creating branches. 
To do so (if you know the Issue you'll work on) click Issues, select the Issue you want to create a branch for, and in the right sidebar (under Development) click Create a branch. 

