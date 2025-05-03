public interface IInstallationService
{
    Task<bool> InstallAsync(SoftwarePackage package, IProgress<int> progress);
    Task<bool> UninstallAsync(string packageId);
    Task<InstallationStatus> GetInstallationStatus(string packageId);
    Task<List<SoftwarePackage>> GetInstalledPackages();
}