# Contributing to Metafacture Playground

Welcome! Thank you for contributing to Metafacture Playground.

The following is a set of guidelines for contributing to Metafacture Playground and how to work together in an efficient and goal-oriented way. We use the simple GitHub workflow: the main branch is always the version that is actually deployed to production. New features are developed in feature branches which are merged into the main after review in pull requests. See details on the [GitHub flow](https://guides.github.com/introduction/flow/). The agile methods we use are inspired by the [Scrum Guide](https://www.scrum.org/resources/scrum-guide).

## Table of Contents

[Board and Tickets](#board-and-tickets)

* [From Backlog to Done](#from-backlog-to-done)
* [Definition of Ready](#definition-of-ready)
* [Definition of Done](#definition-of-done)

[How can I contribute?](#how-can-i-contribute)

* [Reporting Bugs](#reporting-bugs)
* [Suggesting Enhancements](#suggesting-enhancements)
* [Your First Code Contribution](#your-first-code-contribution)
* [Pull Requests](#pull-requests)

[Conventions](#conventions)

* [Git](#git)
* [Commit Messages](#commit-messages)
* [Force Pushing](#force-pushing)

## Board and Tickets

Here is described when tickets are ready and what stages to pass to make a ticket done.

### From Backlog to Done

We track the progress of the Metafacture Playground issues in the [Metafacture Fix and Playground Board](https://github.com/orgs/metafacture/projects/2). Issues move from left to right. We use the following columns:

#### Backlog

Here are all issues that are planned but not ready, have open questions and/or dependencies on other issues or on an external resource. We don't want to write down every idea about the playground so our backlog remains manageable. We are convinced that important issues that can't be implemented now will pop up again so we don't need to write down everything that's important but not feasible now.

#### Ready

An issue is ready if it’s possible to start working on it, i.e. there are no blocking dependencies and requirements are clear enough to start working. Dependencies are expressed through simple referencing of the blocking issue (e.g. depends on #111), see details on [autolinked references and URLs](https://docs.github.com/en/github/writing-on-github/autolinked-references-and-urls). Prioritized items (like bugs) are moved to the top of the *Ready* column. The assignee must verify the readiness with the [Definition of Ready](#definition-of-ready).

#### Working

When we start working on an issue, we move it to the working column. Ideally, every person should only work on one issue at a time. That way the working column provides an overview of who is currently working on what. Issues are only moved into or out of the working column by the person who is assigned. Issues in working are only reassigned by the person who is currently assigned. For every issue we open a feature branch that contains the corresponding issue number and additional info for convenience (using camelCaseFormatting, e.g. 111-featureDesciption). We include references to the corresponding issue in the commit messages. If the assignee thinks the issue is ready for review they move it to the *Review* column and assign a user for functional review. We add instructions and links for testing the changed behavior on the test system in the issue.

#### Review

There are two kinds of reviews:

##### Functional Review

In functional review, the actual behavior of the bugfix or the new feature is reviewed. If the reviewers find problems or have comments during the review, they describe the issues providing links or screenshots that show the behavior, and reassign the team member that submitted the issue, leaving the issue in the review column. If everything works as expected, the reviewers post a +1 comment on the issue and unassign themselves as reviewer.

##### Code Review

In code review, the technical implementation of the bugfix or the new feature is reviewed. To start a code review the person who worked on the issue assigns the corresponding pull request for code review. The pull request should be linked to the issue. Changes during the review process are created in additional commits which are pushed to the feature branch. They are added to the existing pull request automatically. At the end of the code review, the reviewer approves the pull request and reassigns the pull request to its original creator.

#### Done

The creator of the pull request merges the pull request after checking the [definiton of done](#definition-of-done). After the merge, the issue and the linked pull request move to the done column automatically. We delete feature branches after merging.

### Definition of Ready

The *Definition of Ready* describes a list of criteria which tickets have to meet to move from column 'Backlog' to 'Ready':

- The person who implements the ticket is assigned and has every information to work on this ticket. Only the assignee can move the ticket to Ready.
- The implementation of the ticket is planned for the next two (?) weeks and all participants have sufficient resources for the ticket.
- Choose reviewers already in grooming?

### Definition of Done

The *Definition of Done* describes a list of criteria which tickets have to meet to be called 'Done':

- CI passed (CI contains tests)
- Code reviewed
- Functionality reviewed (approved by user/product owner)

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

Before creating enhancement suggestions, please **perform a [cursory search](https://github.com/search?q=is%3Aissue+repo%3Ametafacture%2Fmetafacture-playground)** to see if the enhancement has already been suggested. If it has, add a :+1: to the existing issue instead of opening a new one. When you are creating an enhancement suggestion, please include as many details as possible:

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

After you submit your pull request, verify that all [GitHub Actions](https://docs.github.com/en/actions) are passing. If an action is failing, and you believe that the failure is unrelated to your change, please leave a comment on the pull request explaining why you believe the failure is unrelated. If the failure was a false positive, we will open an issue to track that problem with the GitHub Actions.

The reviewer(s) may ask you to complete additional design work, tests, or other changes before your pull request can be ultimately accepted.

## Conventions

### Git

Git commits should be as granular as possible. When working on a fix for issue X, we try not to add other things we notice (typos, formatting, refactorings, etc.) to the same commit. We don’t use the GitHub shortcuts for closing issues from commits (like fixes #111), since in our process the issue is not solved by the commit but by the reviewed change after it’s deployed to production.

### Commit Messages

* Use the present tense ("Add feature" not "Added feature")
* Use the imperative mood ("Move cursor to..." not "Moves cursor to...")
* Use short lines (max 72 chars), and either just one line, or one line, a blank line, and one or more paragraphs
* Reference issues after the first line ("See #23")

For details, see [these](https://chris.beams.io/posts/git-commit/) [posts](https://tbaggery.com/2008/04/19/a-note-about-git-commit-messages.html).

### Force Pushing

As a general rule, we don't change public commit history, i.e. we don’t use ```--force``` or ```-f``` with ```git push```. Local amending and rebasing before pushing to GitHub is no problem and will not require to ```--force``` when pushing. While we consider this general rule as directive, we condone force pushing as long as the branch has no open pull request yet and only one person is working on this branch. In case of a force push we use ```--force-with-lease``` to ensure that we do not overwrite any remote commits. If rewriting is required in an open pull request, instead of force pushing we open a new branch based on main and ```cherry-pick``` commits or add new code in this branch. The existing pull request is then closed.
