# Contributing guidelines

If you would like to contribute code to Fluxo, you can do so through GitHub by
forking the repository and sending a pull request.

Please make every effort to follow existing conventions and style to keep the code
as readable as possible.

Please also make sure your code is ready by running:<br>
`./gradlew check`.

- If updated dependencies, don't forget to call `./gradlew dependencyGuardBaseline`

- Get working code on a personal branch with all tests before you submit a PR.

- Please, try to keep the git history flat and clean. Avoid merge commits (except for hotfix branches).

- Don’t change public API lightly, avoid if possible, and include your reasoning in the PR if essential.
  It causes pain for developers who use Fluxo and sometimes runtime errors.

- Fluxo is a small and light dependency.
  Don't introduce new dependencies or major new functionality.

- Use [conventional commits](https://conventionalcommits.org/) specification for commit messages.
  Please use this format for PR titles too.

**The expected commit message format is:**

> type(scope): description
* The `type` MUST be one of: `feat`, `fix`, `test`, `build`, `ci`, `docs`, `perf`, `refactor`, `style`, `chore`, `i18n`, `deps`, `misc`, `revert`.
* The `scope` is optional, but recommended. Any string is allowed; it should indicate what the change affects.
* The `description` MUST be pithy and direct short summary of the code changes.
* The `description` MUST be in the imperative, present tense: "change" not "changed" nor "changes".
* A longer commit body MAY be provided after the short description, providing additional contextual information about the code changes. The body MUST begin one blank line after the description.
* One or more footers MAY be provided one blank line after the body. Each footer MUST consist of a word token, followed by either a :<space> or <space># separator, followed by a string value.
* The `BREAKING CHANGE` footer MAY be provided if this commit introduces a breaking change. The `BREAKING CHANGE` footer MUST contain a description of the breaking changes, a justification for why the breaking changes were introduced and any migration notes required.
* See [conventionalcommits.org](https://conventionalcommits.org/) for more details.

<small>_The [commitizen CLI](https://github.com/commitizen/cz-cli) can help to construct these commit messages (but it's not required)._</small>

Examples:
* `build(deps): bump Name Of Lib version to "17.0.2"`
* `docs: fix a typo in README.md`
