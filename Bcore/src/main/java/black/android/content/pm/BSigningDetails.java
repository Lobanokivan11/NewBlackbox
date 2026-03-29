package black.android.content.pm;

import android.content.pm.Signature;
import top.niunaijun.blackreflection.annotation.BClassName;
import top.niunaijun.blackreflection.annotation.BConstructor;

@BClassName("android.content.pm.PackageParser$SigningDetails")
public interface BSigningDetails {
    @BConstructor
    Object _new(Signature[] signatures, int installRequirementType, Object pastSigningCertificates, Object flags);
}
