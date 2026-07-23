# Cloud Integration (P2)

## Capability
Browse and stream from cloud providers: Google Drive, Dropbox, OneDrive.

## Approach
- OAuth per provider; store tokens encrypted.
- List + stream files via provider SDK/REST with range requests for seeking.
- Optional download-for-offline.

## Edge cases
- Token refresh/expiry; revoked access.
- Large-file streaming + seeking over range requests.
- Provider rate limits.
- Format support identical to local (same engine), but no local metadata retriever → limited thumbnails.

## Acceptance criteria
- [ ] Connect a provider, browse, and stream a video with seeking.
- [ ] Token refresh handled silently; revocation recoverable.
