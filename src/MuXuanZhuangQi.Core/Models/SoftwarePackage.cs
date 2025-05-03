public class SoftwarePackage
{
    public string Id { get; set; }
    public string Name { get; set; }
    public string Version { get; set; }
    public string InstallPath { get; set; }
    public long Size { get; set; }
    public string Description { get; set; }
    public Dictionary<string, string> Requirements { get; set; }
    public List<string> Dependencies { get; set; }
    
    public List<InstallationType> SupportedInstallationTypes { get; set; }
    public InstallationType SelectedInstallationType { get; set; }
}