using System;
using System.Collections.Generic;
using System.Text;
using Microsoft.WindowsAzure.Storage.Table;

namespace CounterFunctions
{
    public class PlateNumber : TableEntity
    {
        public String Id { get; set; }
    }
}