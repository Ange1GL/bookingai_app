export const environment = {
  production: true,
  // Empty on purpose: CloudFront serves the SPA (S3) and proxies /api/* to API Gateway on the same
  // origin, so services call `/api/v1/...` relatively. See docs/aws-deployment.md.
  apiBaseUrl: '',
};
