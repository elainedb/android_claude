# Firebase Configuration Setup

This project uses Firebase for Google Sign-In authentication. The `google-services.json` configuration file contains sensitive project information and should not be committed to version control.

## Local Development Setup

1. **Download your google-services.json:**
   - Go to the [Firebase Console](https://console.firebase.google.com/)
   - Select your project
   - Go to Project Settings > General tab
   - Download the `google-services.json` file for your Android app

2. **Place the file:**
   - Copy `google-services.json` to `app/google-services.json`
   - This file is automatically ignored by git and won't be committed

3. **Enable Google Sign-In:**
   - In Firebase Console, go to Authentication > Sign-in method
   - Enable Google Sign-In provider
   - Add your app's SHA-1 fingerprint (use `keytool` command to get it)

## CI/CD Environment

For automated builds and testing, the project automatically generates a dummy `google-services.json` file:

1. **Automatic Generation:**
   - The Gradle build automatically creates `google-services.json` from the template if it doesn't exist
   - This happens before any task that requires Google Services

2. **Template File:**
   - `app/google-services.json.template` contains a dummy configuration
   - This file is committed to version control and used for CI builds
   - Contains placeholder values that allow compilation but won't work for actual authentication

## File Structure

- `google-services.json.template` - Template with dummy values (committed to git)
- `google-services.json` - Real Firebase configuration (**NOT committed to git**)

## Gradle Tasks

- `generateDummyGoogleServices` - Manually generate dummy google-services.json from template
- This task runs automatically before any Google Services processing task

## Troubleshooting

### CI Build Fails with "File google-services.json is missing"
- Ensure `google-services.json.template` exists and is committed
- The Gradle task should automatically generate the file before build

### Local Build Fails
- Download the real `google-services.json` from Firebase Console
- Place it in the `app/` directory
- Verify your Firebase project is properly configured

### Authentication Doesn't Work in CI
- This is expected - CI uses dummy configuration
- Authentication testing should use mock objects or be disabled in CI
- Real authentication testing should be done with local/staging environments

## Security Notes

- Never commit the real `google-services.json` file
- The template file contains only dummy data and is safe to commit
- Actual Firebase configuration should be managed separately for each environment