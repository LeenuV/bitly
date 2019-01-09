/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nic.bit;

/**
 *
 * @author nitin
 */
import java.util.HashSet;

enum CheckType {
	SURBL, CONNECTION, WHITELIST;

	static HashSet<CheckType> parseFlags(final String value) {
		final HashSet<CheckType> set = new HashSet<CheckType>();
		final String[] toks = value.split(",");
		for (final String tok : toks) {
			final String tu = tok.trim().toUpperCase();
			try {
				final CheckType c = CheckType.valueOf(tu);
				set.add(c);
			} catch (Exception e) {
				TinyURL.log.error("Error in flag: " + tu);
			}
		}
		return set;
	}
}