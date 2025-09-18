# Configuration Setup

This application uses a configuration system to manage authorized email addresses and YouTube API keys without committing sensitive data to version control.

## Local Development Setup

1. **Copy the template file:**
   ```bash
   cp app/src/main/assets/config.properties.template app/src/main/assets/config.properties
   ```

2. **Edit the config.properties file:**
   - Open `app/src/main/assets/config.properties`
   - Replace the example emails with your actual authorized email addresses
   - Replace `YOUR_YOUTUBE_API_KEY_HERE` with your actual YouTube Data API key
   - Format:
     ```properties
     authorized_emails=email1@domain.com,email2@domain.com,email3@domain.com
     youtubeApiKey=YOUR_ACTUAL_YOUTUBE_API_KEY
     ```

3. **The config.properties file is automatically ignored by git** and won't be committed.

## CI/CD Environment

For automated builds and testing, the app will automatically fall back to:
1. `config.properties.ci` (contains dummy test emails and API key)
2. `config.properties.template` (contains example emails and placeholder API key)
3. Hard-coded fallback values if no config files are found

## File Structure

- `config.properties.template` - Template file (committed to git)
- `config.properties.ci` - CI configuration (committed to git)
- `config.properties` - Local configuration (**NOT committed to git**)

## Configuration Loading Priority

The app loads configuration in this order:
1. `config.properties` (local development)
2. `config.properties.ci` (CI environment)
3. `config.properties.template` (fallback template)
4. Hard-coded fallback emails (last resort)

## Adding New Environments

To add a new environment configuration:
1. Create `config.properties.{environment}` file
2. Add the file to the loading order in `ConfigHelper.kt`
3. Update `.gitignore` if the file should not be committed