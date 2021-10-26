using System;
using System.Collections.Generic;
using System.IdentityModel.Tokens.Jwt;
using System.Linq;
using System.Security.Claims;
using System.Text;
using Microsoft.IdentityModel.Tokens;

namespace CounterFunctions
{
    internal class AzureSignalR
    {
        private readonly string _endpoint;
        private readonly string _accessKey;

        private static readonly JwtSecurityTokenHandler JwtTokenHandler = new JwtSecurityTokenHandler();

        public AzureSignalR(string connectionString)
        {
            ParseConnectionString(connectionString, out _endpoint, out _accessKey);
        }

        public string GetClientHubUrl(string hubName)
        {
            return $"{_endpoint}/client/?hub={hubName}";
        }

        public string GenerateAccessToken(string audience, string user = null)
        {
            return GenerateAccessTokenInternal(audience, GenerateClaims(user), TimeSpan.FromDays(1));
        }

        private static void ParseConnectionString(string connectionString, out string endpoint, out string accessKey)
        {
            var dict = connectionString.Split(new[] { ';' }, StringSplitOptions.RemoveEmptyEntries).Select(p => p.Split(new[] { '=' }, 2)).ToDictionary(p => p[0].Trim().ToLower(), p => p[1].Trim());
            if (!dict.TryGetValue("endpoint", out endpoint)) throw new ArgumentException("Invalid connection string, missing endpoint.");
            if (!dict.TryGetValue("accesskey", out accessKey)) throw new ArgumentException("Invalid connection string, missing access key.");
        }

        private string GenerateAccessTokenInternal(string audience, IEnumerable<Claim> claims, TimeSpan lifetime)
        {
            var expire = DateTime.UtcNow.Add(lifetime);

            var securityKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(_accessKey));
            var credentials = new SigningCredentials(securityKey, SecurityAlgorithms.HmacSha256);

            var token = JwtTokenHandler.CreateJwtSecurityToken(
                issuer: null,
                audience: audience,
                subject: claims == null ? null : new ClaimsIdentity(claims),
                expires: expire,
                signingCredentials: credentials);
            return JwtTokenHandler.WriteToken(token);
        }

        private IEnumerable<Claim> GenerateClaims(string user)
        {
            if (string.IsNullOrEmpty(user))
                return null;

            return new[] { new Claim(ClaimTypes.NameIdentifier, user) };
        }
    }
}