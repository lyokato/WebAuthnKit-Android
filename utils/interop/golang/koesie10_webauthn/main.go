package main

import (
	"crypto/x509"
	"encoding/base64"
	"fmt"

	"github.com/koesie10/webauthn/protocol"
)

func main() {

	rpId := "https://example.org"
	rpOrigin := "https://example.org"

	assertionChallenge, err := base64.RawURLEncoding.DecodeString("rtnHiVQ7")
	if err != nil {
		fmt.Printf("Challenge Format Error: %v", err)
		return
	}

	b64Id := "bR-PgWS9SeyZjnNv_Wcnyw"
	rawId, err := base64.RawURLEncoding.DecodeString(b64Id)
	if err != nil {
		fmt.Printf("ID Format Error: %v", err)
		return
	}

	attsClientData, err := base64.RawURLEncoding.DecodeString("eyJjaGFsbGVuZ2UiOiJydG5IaVZRNyIsIm9yaWdpbiI6Imh0dHBzOi8vZXhhbXBsZS5vcmciLCJ0eXBlIjoid2ViYXV0aG4uY3JlYXRlIn0")
	if err != nil {
		fmt.Printf("ClientData Format Error: %v", err)
		return
	}

	attsObj, err := base64.RawURLEncoding.DecodeString("o2hhdXRoRGF0YVkAlFDXqQXjBGuIY4NizDSjGhrlNHZspV46o5eVHv5lOwYrRQAAAAAAAAAAAAAAAAAAAAAAAAAAABBtH4-BZL1J7JmOc2_9ZyfLpQECAyYgASFYIC4nb69yT0foGdhsfExeazBqYjCCpDDg1tPvZDbe6x9kIlggGnBTJBEU8lOjV9SBAiw6jg2sKbVSCafh8iISdoOI2V9jZm10a2FuZHJvaWQta2V5Z2F0dFN0bXSjY3NpZ1hGMEQCIFos-IECGLA6az1jqUTlhCYQ9CcpFgPsdNBYo54zjzTlAiAYhG48toA10_WOKNd833--rcSI-GbZ2qllPW_Z2TIZI2N4NWOEWQKTMIICjzCCAjWgAwIBAgIBATAKBggqhkjOPQQDAjApMRkwFwYDVQQFExAwZmZmYTlkOTZiMjVjMDcwMQwwCgYDVQQMDANURUUwIBcNNzAwMTAxMDAwMDAwWhgPMjEwNjAyMDcwNjI4MTVaMB8xHTAbBgNVBAMMFEFuZHJvaWQgS2V5c3RvcmUgS2V5MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAELidvr3JPR-gZ2Gx8TF5rMGpiMIKkMODW0-9kNt7rH2QacFMkERTyU6NX1IECLDqODawptVIJp-HyIhJ2g4jZX6OCAVQwggFQMA4GA1UdDwEB_wQEAwIHgDCCATwGCisGAQQB1nkCAREEggEsMIIBKAIBAwoBAQIBBAoBAQQgZwyITF2cEjrwfaEoKPlsZJ7eOUiX2vWxRhSGqxxTXBwEADBUv4U9CAIGAWh55Ladv4VFRARCMEAxGjAYBBN3ZWJhdXRobmtpdC5leGFtcGxlAgEBMSIEICvJSr2SebWg4SqdaqNGgNMHbmN1gPgn48TgtNbowFbKMIGfoQUxAwIBAqIDAgEDowQCAgEApQUxAwIBBKoDAgEBv4N3AgUAv4U-AwIBAL-FQEwwSgQgt5k5Gvrjs1Ui0e3Fxwo3RrCXvdHKvVn3K7BJcFx6A-8BAf8KAQAEIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAv4VBBQIDAV-Qv4VCBQIDAxStv4VOBQIDAxRRv4VPBQIDAxStMAoGCCqGSM49BAMCA0gAMEUCIQDnfUlXVi4rjiIYZPWthGj5cvOmEgNaKBnWLPeDFFAr3wIgYYe9mz7s7a0euklgbBfsLpVR8B-TZVutjaqIIW17CipZAigwggIkMIIBq6ADAgECAgoEdUmWWElAVkkIMAoGCCqGSM49BAMCMCkxGTAXBgNVBAUTEGI3ZTg5ZWRhNWM3ZTdkMGIxDDAKBgNVBAwMA1RFRTAeFw0xODA5MjAyMjI3MDZaFw0yODA5MTcyMjI3MDZaMCkxGTAXBgNVBAUTEDBmZmZhOWQ5NmIyNWMwNzAxDDAKBgNVBAwMA1RFRTBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABIirpxQtmac6WZlvkE3oOm1Xn66Jp9SXM42v_gb_rPoKaEQ_CrHPBpNaYk7Du2NGPySx7qzP1cY_wbw88aWxPaOjgbowgbcwHQYDVR0OBBYEFMjg8a-lXb_iZgHnNnNRzlZmq-JoMB8GA1UdIwQYMBaAFKs5ingeaudD_mbVM6BNQHdHTF-oMA8GA1UdEwEB_wQFMAMBAf8wDgYDVR0PAQH_BAQDAgIEMFQGA1UdHwRNMEswSaBHoEWGQ2h0dHBzOi8vYW5kcm9pZC5nb29nbGVhcGlzLmNvbS9hdHRlc3RhdGlvbi9jcmwvMDQ3NTQ5OTY1ODQ5NDA1NjQ5MDgwCgYIKoZIzj0EAwIDZwAwZAIwW6G3UBvvjqzpRnQlJ2fB1EPiKM8pYFWpzswThzWYwX5AVytdrvkvR4oJemY8KmjbAjBR1HYMK5P9ubJdZOJU6LtvtFD9JY1djIePuyS8tQWzegnUqhWv4p8HjyeiOCAyUP5ZA9UwggPRMIIBuaADAgECAgoDiCZnYGWJloWoMA0GCSqGSIb3DQEBCwUAMBsxGTAXBgNVBAUTEGY5MjAwOWU4NTNiNmIwNDUwHhcNMTgwOTIwMjIyNDQ1WhcNMjgwOTE3MjIyNDQ1WjApMRkwFwYDVQQFExBiN2U4OWVkYTVjN2U3ZDBiMQwwCgYDVQQMDANURUUwdjAQBgcqhkjOPQIBBgUrgQQAIgNiAARuWKgChO18aGw_bMjGOaygo2ILC1KilOOvEbNtkeA6scf17ap22daRwjs1Aaka_aI4WkhjZxNYf9bcB6mlvhk-pZrPGHNPmO6Pb81jar3I7lQDSwWevT4blYG71w-_fkWjgbYwgbMwHQYDVR0OBBYEFKs5ingeaudD_mbVM6BNQHdHTF-oMB8GA1UdIwQYMBaAFDZh4QB8iAUJUYtEbEf_GkzJ6k8SMA8GA1UdEwEB_wQFMAMBAf8wDgYDVR0PAQH_BAQDAgIEMFAGA1UdHwRJMEcwRaBDoEGGP2h0dHBzOi8vYW5kcm9pZC5nb29nbGVhcGlzLmNvbS9hdHRlc3RhdGlvbi9jcmwvRThGQTE5NjMxNEQyRkExODANBgkqhkiG9w0BAQsFAAOCAgEAIhqPYMyULWT4WvG5DXVM60L2n_dcsmOpuHygv6LkT2dCzNl6HHzHMk5ui6ncvtSdIILA_zEVJctQpr795M4NWHBhi6ZWTIilpB26tqNzPSyGTTFewLl0iwLQWUNu42Y2_p4TKZ3emhwlGoeATgoQvjth7mr5xDOaNBFM0evWqtBmdzddf7psAZq9udJvs3BeziOM1gy6VWm-FMnA7UuG--a5OdPihRP9XLamGBpbEfgCx-fIiHkgQ-G3BVKuqyr6YgqcFF2P3pibXKGY65hVB_vxti8hiN42OeDlgzcImS3sDb9tWHe9OcSPCCrec3TX72mC6OsGWQyFlcoV4fGiud2L65RJp_EYKfg5mdQehUZjVX-RhZkVjIE7sLV1HWd02S67f2yb8c4W-Q6MOQyl2tsXxBSdgRTeDS510Amlwcy4I93eIuGyplU4Cz9qNn5zmYQkbTs9hG9nRMUNNlpILKfdE_A0Ul1SKv1b7GlYDE1IRYCeXPo_321faEcONAMnmKjhLZ06q5jR2KqJt2M9h1fHWc6bOzLnshlXDq0Wa0N_ghStFdrdjzQbRcmA5eMJgzZhLQGppNv2fBJb4eHRSzAyH1nOAN2fk1JSvK97DU9kSnynNmIC-lfCI2ekEbvMygnQ7TuQKXBqIl53VL4BRFS_bbx-uehG9WzmD2KWaPRZBWQwggVgMIIDSKADAgECAgkA6PoZYxTS-hgwDQYJKoZIhvcNAQELBQAwGzEZMBcGA1UEBRMQZjkyMDA5ZTg1M2I2YjA0NTAeFw0xNjA1MjYxNjI4NTJaFw0yNjA1MjQxNjI4NTJaMBsxGTAXBgNVBAUTEGY5MjAwOWU4NTNiNmIwNDUwggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAwggIKAoICAQCvtseCK7GnAewrtC6LzFQWY6vvmC8yx391MQMMl1JLG1_oCfvHKqlFH3Q8vZpvEzV0SqVed_a2rDU17hfCXmOVF92ckuY3SlPL_iWPj_u2_RKTeKIqTKmcRS1HpZ8yAfRBl8oczX52L7L1MVG2_rL__Stv5P5bxr2ew0v-CCOdqvzrjrWo7Ss6zZxeOneQ4bUUQnkxWYWYEa2esqlrvdelfJOpHEH8zSfWf9b2caoLgVJhrThPo3lEhkYE3bPYxPkgoZsWVsLxStbQPFbsBgiZBBwe0aX-bTRAtVa60dChUlicU-VdNwdi8BIu75GGGxsObEyAknSZwOm-wLg-O8H5PHLASWBLvS8TReYsP44m2-wGyUdm88EoI51PQxL62BI4h-Br7PVnWDv4NVqB_uq6-ZqDyN8-KjIq_Gcr8SCxNRWLaCHOrzCbbu53-YgzsBjaoQ5FHwajdNUHgfNZCClmu3eLkwiUJpjnTgvNJGKKAcLMA-UfCz5bSsHk356vn_akkqd8FI")
	if err != nil {
		fmt.Printf("Attestation Format Error: %v", err)
		return
	}

	assertionClientData, err := base64.RawURLEncoding.DecodeString("")
	if err != nil {
		fmt.Printf("ClientData Format Error: %v", err)
		return
	}

	authData, err := base64.RawURLEncoding.DecodeString("")
	if err != nil {
		fmt.Printf("AuthData Format Error: %v", err)
		return
	}
	signature, err := base64.RawURLEncoding.DecodeString("")
	if err != nil {
		fmt.Printf("Signature Format Error: %v", err)
		return
	}
	userHandle, err := base64.RawURLEncoding.DecodeString("bHlva2F0bw")
	if err != nil {
		fmt.Printf("UserHandle Format Error: %v", err)
		return
	}

	attsRes := protocol.AttestationResponse{
		PublicKeyCredential: protocol.PublicKeyCredential{
			ID:    b64Id,
			RawID: rawId,
			Type:  "public-key",
		},
		Response: protocol.AuthenticatorAttestationResponse{
			AuthenticatorResponse: protocol.AuthenticatorResponse{
				ClientDataJSON: attsClientData,
			},
			AttestationObject: attsObj,
		},
	}

	fmt.Println("Parse Attestation Response")
	atts, err := protocol.ParseAttestationResponse(attsRes)
	if err != nil {
		e := protocol.ToWebAuthnError(err)
		fmt.Printf("Error: %s, %s, %s", e.Name, e.Debug, e.Hint)
		return
	}

//	 This returns err, because this webauthn library doesn't support self-attestation
	validAtts, err := protocol.IsValidAttestation(atts, assertionChallenge, rpId, rpOrigin)
	if err != nil {
		e := protocol.ToWebAuthnError(err)
		fmt.Printf("Error: %s, %s, %s", e.Name, e.Debug, e.Hint)
		return
	}
	if !validAtts {
		fmt.Println("Invalid Attestation!")
		return
	}

	return

	pubKey := atts.Response.Attestation.AuthData.AttestedCredentialData.COSEKey

	cert := &x509.Certificate{
		PublicKey: pubKey,
	}

	assertionRes := protocol.AssertionResponse{
		PublicKeyCredential: protocol.PublicKeyCredential{
			ID:    b64Id,
			RawID: rawId,
			Type:  "public-key",
		},
		Response: protocol.AuthenticatorAssertionResponse{
			AuthenticatorResponse: protocol.AuthenticatorResponse{
				ClientDataJSON: assertionClientData,
			},
			AuthenticatorData: authData,
			Signature:         signature,
			UserHandle:        userHandle,
		},
	}
	assertion, err := protocol.ParseAssertionResponse(assertionRes)
	if err != nil {
		e := protocol.ToWebAuthnError(err)
		fmt.Printf("Error: %s, %s, %s", e.Name, e.Debug, e.Hint)
		return
	}

	valid, err := protocol.IsValidAssertion(assertion, assertionChallenge, rpId, rpOrigin, cert)
	if err != nil {
		e := protocol.ToWebAuthnError(err)
		fmt.Printf("Error: %s, %s, %s", e.Name, e.Debug, e.Hint)
		return
	}

	if !valid {
		fmt.Println("Invalid Assertion!")
		return
	}

	fmt.Println("Valid Assertion!!!")

}
