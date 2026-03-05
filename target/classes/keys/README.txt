Place CKYC key material in this folder for local runtime.

Generated dummy material in this folder:
- fi-keystore.p12
- project-public.cer
- project-private.key
- ckyc-keystore.p12
- ckyc-public.cer
- ckyc-private.key
- cersai.cer (copied from ckyc-public.cer for dummy run)

Required by current code (active path priority):
- cersai.cer          : CKYC/CERSAI public certificate used for RSA session key encryption
- fi-keystore.p12     : FI PKCS12 containing private key + certificate for XML digital signature
- ckyc-public.cer     : preferred CKYC public certificate path (if configured)
- project-private.key : preferred FI private key path (if configured)
- project-public.cer  : preferred FI certificate path (if configured)

Optional for dummy/mock upstream:
- ckyc-private.key
- ckyc-keystore.p12

Default values are configured in `src/main/resources/application.properties`.
