# oereb-checker
Code + Docker/ECR for swisstopo oereb-checker

## CI/CD - Docker Build and Push to ECR

The repository is configured with a GitHub Actions workflow that automatically builds and pushes Docker images to AWS ECR.

### Automatic Triggers

The workflow automatically triggers on pushes to the following branches when changes are made to:
- `docker-oereb/**`
- `src/main/**`
- `pom.xml`

**Enabled branches:**
- `main` - Production builds with tags: `latest`, `main`, `main-<version>`
- `fix/lambda-debugging-improvements` - Feature branch builds with tags: `fix-lambda-debugging-improvements`, `fix-lambda-debugging-improvements-<version>`

**Note:** Feature branches are added to automatic triggers on a case-by-case basis. For ad-hoc testing, use the manual workflow dispatch option below.

### Manual Trigger

You can also manually trigger a build from any branch using GitHub's workflow dispatch:

1. Go to the [Actions tab](https://github.com/swisstopo/oereb-checker/actions)
2. Select the "Docker Build and Push to ECR" workflow
3. Click "Run workflow"
4. Select the branch you want to build
5. Click "Run workflow" to start the build

### Docker Image Tags

Images are pushed to ECR with the following tagging strategy:

- **Main branch:** 
  - `<ecr-repo>:latest` (always points to the most recent main build)
  - `<ecr-repo>:main`
  - `<ecr-repo>:main-<version>`
  
- **Feature branches:** 
  - `<ecr-repo>:<sanitized-branch-name>`
  - `<ecr-repo>:<sanitized-branch-name>-<version>`
  
Branch names are sanitized by replacing invalid characters with hyphens (e.g., `fix/lambda-debugging-improvements` becomes `fix-lambda-debugging-improvements`).

### Testing with Feature Branch Images

To test your Lambda with an image from a feature branch:

1. Push your changes to the feature branch (e.g., `fix/lambda-debugging-improvements`)
2. The workflow will automatically build and push the image to ECR (if the branch is in the enabled list) or manually trigger the workflow via workflow dispatch
3. Use the ECR image with the sanitized branch name tag (e.g., `fix-lambda-debugging-improvements`) or versioned tag (e.g., `fix-lambda-debugging-improvements-<version>`) in your Lambda configuration

**Note:** 
- GitHub Releases are only created for builds from the `main` branch
- The `latest` tag always points to the most recent `main` branch build and is not updated by feature branch builds
