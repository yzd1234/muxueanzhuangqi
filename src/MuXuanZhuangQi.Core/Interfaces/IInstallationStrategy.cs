public interface IInstallationStrategy
{
    Task<bool> Install(SoftwarePackage package, IProgress<int> progress);
    Task<bool> Uninstall(string packageId);
    bool IsSupported(SoftwarePackage package);
}