# Contributing to Metafacture Playground

Welcome! Thank you for contributing to Metafacture Playground.

The following is a set of guidelines for contributing to Metafacture Playground and how to work together in an efficient and goal-oriented way. We use the simple GitHub workflow: the main branch is always the version that is actually deployed to production. New features are developed in feature branches which are merged into the main after review in pull requests. See details on the [GitHub flow](https://guides.github.com/introduction/flow/). The agile methods we use are inspired by the [Scrum Guide](https://www.scrum.org/resources/scrum-guide).

## Table of Contents

[How can I contribute?](#how-can-i-contribute)
* [Reporting Bugs](#reporting-bugs)
* [Suggesting Enhancements](#suggesting-enhancements)
* [Pull Requests](#pull-requests)
* [Conventions](#conventions)

[Maintainer Guidelines](#maintainer-guidelines)
* [Board and Issues](#board-and-issues)
* [From Backlog to Done](#from-backlog-to-done)
* [Definition of Ready](#definition-of-ready)
* [Definition of Done](#definition-of-done)


## How can I contribute?

### Reporting Bugs

This section guides you through submitting a bug report for the Metafacture Playground. Following these guidelines helps maintainers and the community understand your report, reproduce the behavior and find related reports.

Before creating bug reports, please check if an issue with this bug already exists. When you are creating a bug report, please [include as many details as possible](#how-do-i-submit-a-bug-report).

**Note:** If you find a **Closed** issue that seems like it is the same thing that you're experiencing, open a new issue and include a link to the original issue in the body of your new one.

#### How Do I Submit A Bug Report?

Bugs are tracked as [GitHub issues](https://guides.github.com/features/issues/). Create a [new issue on the repository](https://github.com/metafacture/metafacture-playground/issues/new).

Explain the problem and include additional details to help maintainers reproduce the problem:

* **Use a clear and descriptive title** for the issue to identify the problem.
* **Describe the exact steps which reproduce the problem** in as many details as possible. For example, start by explaining which browser you used to visit the playground. When listing steps, **don't just say what you did, but explain how you did it**. For example, if you moved the cursor to the end of a line, explain if you used the mouse or a keyboard shortcut.
* **Provide specific examples to demonstrate the steps**. Include links to files or GitHub projects, or copy/pasteable snippets, which you use in those examples. If you're providing snippets in the issue, use [Markdown code blocks](https://github.com/adam-p/markdown-here/wiki/Markdown-Cheatsheet#code).
* **Describe the behavior you observed after following the steps** and point out what exactly is the problem with that behavior.
* **Explain which behavior you expected to see instead and why.**
* **If the problem wasn't triggered by a specific action**, describe what you were doing before the problem happened.

### Suggesting Enhancements

This section guides you through submitting an enhancement suggestion for the Metafacture Playground, including completely new features and minor improvements to existing functionality. Following these guidelines helps maintainers and the community understand your suggestion and find related suggestions.

Before creating enhancement suggestions, please **perform a [cursory search](https://github.com/search?q=is%3Aissue+repo%3Ametafacture%2Fmetafacture-playground)** to see if the enhancement has already been suggested. If it has, add a comment to the existing issue instead of opening a new one. When you are creating an enhancement suggestion, please include as many details as possible:

#### How Do I Submit An Enhancement Suggestion?

Enhancement suggestions are tracked as [GitHub issues](https://guides.github.com/features/issues/). Create [new issue on the repository](https://github.com/metafacture/metafacture-playground/issues/new) and provide the following information:

* **Use a clear and descriptive title** for the issue to identify the suggestion.
* **Provide a step-by-step description of the suggested enhancement** in as many details as possible.
* **Provide specific examples to demonstrate the steps**. Include copy/pasteable snippets which you use in those examples, as [Markdown code blocks](https://github.com/adam-p/markdown-here/wiki/Markdown-Cheatsheet#code).
* **Describe the current behavior** and **explain which behavior you expected to see instead** and why.
* **Explain why this enhancement would be useful** to most users.
* **List some other applications where this enhancement exists.**
* **Specify the name and version of the browser you're using.**

### Pull Requests

Please follow these steps to [propose contributions to the project](https://docs.github.com/en/github/collaborating-with-issues-and-pull-requests/proposing-changes-to-your-work-with-pull-requests).

When submitting a pull request use a meaningful title and use [closing keywords](https://docs.github.com/en/github/managing-your-work-on-github/linking-a-pull-request-to-an-issue#linking-a-pull-request-to-an-issue-using-a-keyword) to reference the issue it resolves in the pull request description.
After you submit your pull request, verify that all [GitHub Actions](https://docs.github.com/en/actions) are passing. If an action is failing, and you believe that the failure is unrelated to your change, please leave a comment on the pull request explaining why you believe the failure is unrelated. If the failure was a false positive, we will open an issue to track that problem with the GitHub Actions.

The reviewer(s) may ask you to complete additional design work, tests, or other changes before your pull request can be ultimately accepted.

### Conventions

#### Git

Git commits should be as granular as possible. When working on a fix for issue X, we try not to add other things we notice (formatting, refactorings, etc.) to the same commit. Those things should be placed in an own commit to the same branch. If it is necessary for understanding, add something like "Discovered while working on #14" to the commit message.
We don’t use the GitHub shortcuts for closing issues from commits (like fixes #111), since in our process the issue is not solved by the commit but by the reviewed change after it’s deployed to production.

#### Commit Messages

* Use the imperative mood in subject line ("Add feature" not "Added feature")
* Use short lines (max 72 chars), and either just one line, or one line, a blank line, and one or more paragraphs
* Reference issues at the end of the first line
* For referencing issues in the same repository use the short form ("#14" not "https://github.com/metafacture/metafacture-playground/issues/14")
* Don't use closing keywords in commit messages (e.g. "See #14" not "Resolves #14")

For details, see [these](https://chris.beams.io/posts/git-commit/) [posts](https://tbaggery.com/2008/04/19/a-note-about-git-commit-messages.html).

#### Force Pushing

As a general rule, we don't change public commit history, i.e. we don’t use ```--force``` or ```-f``` with ```git push```. Local amending and rebasing before pushing to GitHub is no problem and will not require to ```--force``` when pushing. While we consider this general rule as directive, we condone force pushing as long as the branch has no open pull request yet and only one person is working on this branch. In case of a force push we use ```--force-with-lease``` to ensure that we do not overwrite any remote commits. If rewriting is required in an open pull request, instead of force pushing we open a new branch based on main and ```cherry-pick``` commits or add new code in this branch. The existing pull request is then closed.

#### Browsers

We test the Metafacture Playground against Mozilla Firefox and Google Chrome. The behaviour in these two browsers may be different, but should be acceptable for each of them.

## Maintainer Guidelines

### Board and Issues

We use the [Metafacture Board](https://github.com/orgs/metafacture/projects/3/views/4) to track the progress of issues of the Metafacture Playground. In the following we describe when issues are ready and what stages to pass to make a issue done.

### From Backlog to Done

Issues move from left to right. We use the following columns:

#### Backlog

Here are all issues that are planned but not ready, have open questions and/or dependencies on other issues or on an external resource. We don't want to write down every idea about the playground so our backlog remains manageable. We are convinced that important issues that can't be implemented now will pop up again so we don't need to write down everything that's important but not feasible now.

#### Ready

An issue is ready if it’s possible to start working on it according to the [Definition of Ready](#definition-of-ready). Prioritized items (like bugs) are moved to the top of the *Ready* column. The assignee must re-verify the readiness when moving the item from *Ready* to *Working*, especially when an item has been in the 'Ready'-column for a long time.

#### Working

When we start working on an issue, we move it to the working column. Ideally, every person should only work on one issue at a time. That way the working column provides an overview of who is currently working on what. Issues are only moved into or out of the working column by the person who is assigned. Issues in working are only reassigned by the person who is currently assigned. For every issue we open a feature branch that contains the corresponding issue number and additional info for convenience (using camelCaseFormatting, e.g. '111-featureDesciption'). If the assignee thinks the issue is ready for review they add instructions and links for testing the changed behavior on the test system in the issue, move it to the *Review* column, assign the previously announced functional reviewer (see [Definition of Ready](#definition-of-ready)), and open an unassigned pull request for the feature branch.

#### Review

There are two kinds of reviews: first, a functional review (which happens on the issue) and second, a code review (which happens on the pull request).

##### Functional Review

In functional review, the actual behavior of the bugfix or the new feature is reviewed. If the reviewer finds problems, these should be described by providing links or screenshots that show the behavior, and then reassigns the team member that submitted the issue, leaving the issue in the review column. If everything works as expected, the reviewer posts a +1 comment on the issue, removes the assignment and makes the suggested code reviewer assignee and reviewer of the linked pull request. The issue remains unassigned.

##### Code Review

In code review, the technical implementation of the bugfix or the new feature is reviewed. Changes during the review process are created in additional commits which are pushed to the feature branch. They are added to the existing pull request automatically. At the end of the code review, the reviewer approves the pull request and reassigns the pull request to its original creator.

#### Done

The creator of the pull request merges the pull request after checking the [Definition of Done](#definition-of-done). After the merge, the issue and the linked pull request are closed and moved to the *Done* column automatically (due to the [closing keywords](https://docs.github.com/en/github/managing-your-work-on-github/linking-a-pull-request-to-an-issue#linking-a-pull-request-to-an-issue-using-a-keyword) used in the pull request description). We delete feature branches after merging.

### Definition of Ready

The *Definition of Ready* describes a list of criteria which issues have to meet to move from column 'Backlog' to 'Ready':

- The person who will implement the issue is assigned and has every information to work on this issue. Only the assignee can move the issue to Ready.
- The person who will do functional review is mentioned in the issue (e.g. add a comment like "could be reviewed by ...")
- The person who will do code review is mentioned in the issue (e.g. add a comment like "could be reviewed by ...")
- There are no blocking dependencies. Dependencies are expressed through simple referencing of the blocking issue (e.g. depends on #111), see details on [autolinked references and URLs](https://docs.github.com/en/github/writing-on-github/autolinked-references-and-urls)

### Definition of Done

The *Definition of Done* describes a list of criteria which issues have to meet to be called 'Done':

- Functionality reviewed (approved by user/product owner)
- Documentation exists (external documentation must be linked)
- GitHub Actions / CI passed (contains tests)
- Pull request is reviewed and approved
- Functionality is merged into the main branch
- Deployed to production
