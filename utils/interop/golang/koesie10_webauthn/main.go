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

	b64Id := "zHuGlLXMSnOsej49nntFvw"
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

	attsObj, err := base64.RawURLEncoding.DecodeString("o2hhdXRoRGF0YVkAlFDXqQXjBGuIY4NizDSjGhrlNHZspV46o5eVHv5lOwYrRQAAAAAAAAAAAAAAAAAAAAAAAAAAABDMe4aUtcxKc6x6Pj2ee0W_pQECAyYgASFYIGXuyXv07IAp6p_fJqBmmAp0Gd6ykLF6V2KtyJ1GSvuMIlggoqWs2DMcuZYnFV7Vta8Cnrun1u52bCBlGEp9EVOyM3hjZm10ZnBhY2tlZGdhdHRTdG10omNzaWdYRzBFAiB0gywNL4ndvnGqcB_Zu8GoOBfYwPER6BZIAtZVd4yZOAIhAKc64mCPgk5aE9tqAXR6eHm-l3XYZ3tPz6q_vzk7gomWY2FsZyY")
	if err != nil {
		fmt.Printf("Attestation Format Error: %v", err)
		return
	}

	assertionClientData, err := base64.RawURLEncoding.DecodeString("eyJjaGFsbGVuZ2UiOiJydG5IaVZRNyIsIm9yaWdpbiI6Imh0dHBzOi8vZXhhbXBsZS5vcmciLCJ0eXBlIjoid2ViYXV0aG4uZ2V0In0")
	if err != nil {
		fmt.Printf("ClientData Format Error: %v", err)
		return
	}

	authData, err := base64.RawURLEncoding.DecodeString("UNepBeMEa4hjg2LMNKMaGuU0dmylXjqjl5Ue_mU7BisFAAAAAQ")
	if err != nil {
		fmt.Printf("AuthData Format Error: %v", err)
		return
	}
	signature, err := base64.RawURLEncoding.DecodeString("MEQCIAd6dmmRAfWThFYZ5l2xthciDwnCowwYakUE53nwLvmwAiBQD7Ndu7QwXlNtTHcY0fgZ4A_jzQ78fnDZ_v7Hm6gvDw")
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

	/*
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
	*/

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
